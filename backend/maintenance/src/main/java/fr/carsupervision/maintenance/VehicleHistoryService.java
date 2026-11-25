package fr.carsupervision.maintenance;

import fr.carsupervision.common.time.Today;
import fr.carsupervision.vehicle.AuthorPolicy;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Historique d'un véhicule (US3, FR-014 à FR-016, FR-018) : relevés et entretiens fusionnés du
 * plus récent au plus ancien, et totaux des dépenses calculés en base (centimes, SC-006).
 */
@Service
public class VehicleHistoryService {

    /** Ordre FR-014 : date décroissante, puis kilométrage décroissant ; à égalité, l'entretien d'abord. */
    static final Comparator<Entry> ORDER = Comparator.comparing(Entry::date).reversed()
            .thenComparing(Comparator.comparingInt(Entry::mileageKm).reversed())
            .thenComparing(entry -> entry.reading() != null);

    private final VehicleService vehicleService;
    private final MileageReadingRepository readingRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final AuthorPolicy authorPolicy;
    private final ObjectProvider<RecordDocumentCounter> documentCounter;
    private final Today today;

    public VehicleHistoryService(VehicleService vehicleService, MileageReadingRepository readingRepository,
            MaintenanceRecordRepository recordRepository, AuthorPolicy authorPolicy,
            ObjectProvider<RecordDocumentCounter> documentCounter, Today today) {
        this.vehicleService = vehicleService;
        this.readingRepository = readingRepository;
        this.recordRepository = recordRepository;
        this.authorPolicy = authorPolicy;
        this.documentCounter = documentCounter;
        this.today = today;
    }

    /** Une ligne : un relevé ou un entretien (l'autre champ est nul). */
    public record Entry(MileageReading reading, MaintenanceRecord record, boolean canEdit, int documentCount) {

        LocalDate date() {
            return reading != null ? reading.getReadingDate() : record.getRecordDate();
        }

        int mileageKm() {
            return reading != null ? reading.getMileageKm() : record.getMileageKm();
        }
    }

    public record Totals(int year, long currentYearCents, long allTimeCents) {
    }

    public record History(Vehicle vehicle, Totals totals, List<Entry> entries) {
    }

    /**
     * Mêmes droits que la lecture du véhicule : un USER seulement sur ses véhicules actifs, un
     * ADMIN sur tous, archivés compris. Un filtre par type exclut les relevés.
     */
    @Transactional(readOnly = true)
    public History get(UUID vehicleId, UUID typeId, boolean recordsOnly) {
        Vehicle vehicle = vehicleService.get(vehicleId);
        List<MaintenanceRecord> records = typeId == null
                ? recordRepository.findByVehicleId(vehicleId)
                : recordRepository.findByVehicleIdAndType(vehicleId, typeId);
        Map<UUID, Integer> documents = countDocuments(records);

        List<Entry> entries = new ArrayList<>();
        records.forEach(record -> entries.add(new Entry(null, record, authorPolicy.canEdit(record.getAuthorId()),
                documents.getOrDefault(record.getId(), 0))));
        if (typeId == null && !recordsOnly) {
            readingRepository.findByVehicleIdOrderByReadingDateDescMileageKmDesc(vehicleId)
                    .forEach(reading -> entries.add(
                            new Entry(reading, null, authorPolicy.canEdit(reading.getAuthorId()), 0)));
        }
        entries.sort(ORDER);
        return new History(vehicle, totals(vehicleId), entries);
    }

    private Totals totals(UUID vehicleId) {
        int year = today.year();
        long currentYear = recordRepository.sumCostBetween(vehicleId, LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31));
        return new Totals(year, currentYear, recordRepository.sumCost(vehicleId));
    }

    private Map<UUID, Integer> countDocuments(List<MaintenanceRecord> records) {
        RecordDocumentCounter counter = documentCounter.getIfAvailable();
        if (counter == null || records.isEmpty()) {
            return Map.of();
        }
        return counter.countByRecord(records.stream().map(MaintenanceRecord::getId).toList());
    }
}
