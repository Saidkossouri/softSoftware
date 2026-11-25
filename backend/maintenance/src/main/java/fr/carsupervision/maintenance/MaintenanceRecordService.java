package fr.carsupervision.maintenance;

import fr.carsupervision.api.model.MaintenanceRecordInput;
import fr.carsupervision.common.Normalizers;
import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.time.Today;
import fr.carsupervision.vehicle.AuthorPolicy;
import fr.carsupervision.vehicle.AuthorPolicy.AuthorSnapshot;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleAccessGuard;
import fr.carsupervision.vehicle.VehicleService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Entretiens réalisés (US2, FR-010 à FR-013). Mêmes étapes que les relevés : verrou du véhicule,
 * auteur ou ADMIN, validation, cohérence du kilométrage partagée avec les relevés, puis recalcul
 * du kilométrage actuel.
 */
@Service
public class MaintenanceRecordService {

    static final String NOT_FOUND_MESSAGE = "Cet entretien n'existe pas.";

    private final MaintenanceRecordRepository recordRepository;
    private final MaintenanceTypeRepository typeRepository;
    private final MileagePoints mileagePoints;
    private final CurrentMileageUpdater currentMileageUpdater;
    private final VehicleAccessGuard accessGuard;
    private final VehicleService vehicleService;
    private final AuthorPolicy authorPolicy;
    private final ObjectProvider<RecordDocumentCounter> documentCounter;
    private final ApplicationEventPublisher events;
    private final Today today;
    private final Clock clock;

    public MaintenanceRecordService(MaintenanceRecordRepository recordRepository,
            MaintenanceTypeRepository typeRepository, MileagePoints mileagePoints,
            CurrentMileageUpdater currentMileageUpdater, VehicleAccessGuard accessGuard,
            VehicleService vehicleService, AuthorPolicy authorPolicy,
            ObjectProvider<RecordDocumentCounter> documentCounter, ApplicationEventPublisher events, Today today,
            Clock clock) {
        this.recordRepository = recordRepository;
        this.typeRepository = typeRepository;
        this.mileagePoints = mileagePoints;
        this.currentMileageUpdater = currentMileageUpdater;
        this.accessGuard = accessGuard;
        this.vehicleService = vehicleService;
        this.authorPolicy = authorPolicy;
        this.documentCounter = documentCounter;
        this.events = events;
        this.today = today;
        this.clock = clock;
    }

    /** Entretien prêt à afficher : véhicule (kilométrage actuel), droits et nombre de justificatifs. */
    public record View(MaintenanceRecord record, Vehicle vehicle, boolean canEdit, int documentCount) {
    }

    /** Lecture : mêmes droits que le véhicule (un ADMIN lit aussi un véhicule archivé). */
    @Transactional(readOnly = true)
    public View get(UUID recordId) {
        MaintenanceRecord record = find(recordId);
        Vehicle vehicle = vehicleService.get(record.getVehicleId());
        return view(record, vehicle);
    }

    @Transactional
    public View create(UUID vehicleId, MaintenanceRecordInput input) {
        Vehicle vehicle = accessGuard.lockForWrite(vehicleId);
        AuthorSnapshot author = authorPolicy.authorSnapshot();
        MaintenanceRecord record = new MaintenanceRecord(vehicleId, author.id(), author.name(),
                OffsetDateTime.now(clock));
        apply(input, record, vehicle, null);
        recordRepository.saveAndFlush(record);
        currentMileageUpdater.refresh(vehicle);
        return view(record, vehicle);
    }

    @Transactional
    public View update(UUID recordId, MaintenanceRecordInput input) {
        MaintenanceRecord record = find(recordId);
        Vehicle vehicle = accessGuard.lockForWrite(record.getVehicleId());
        authorPolicy.assertAuthorOrAdmin(record.getAuthorId());
        apply(input, record, vehicle, record);
        record.setUpdatedAt(OffsetDateTime.now(clock));
        recordRepository.saveAndFlush(record);
        currentMileageUpdater.refresh(vehicle);
        return view(record, vehicle);
    }

    /**
     * Supprime l'entretien. L'événement {@link MaintenanceRecordDeleted} est publié avant la
     * suppression de la ligne, pour que les justificatifs partent dans la même transaction.
     */
    @Transactional
    public void delete(UUID recordId) {
        MaintenanceRecord record = find(recordId);
        Vehicle vehicle = accessGuard.lockForWrite(record.getVehicleId());
        authorPolicy.assertAuthorOrAdmin(record.getAuthorId());
        events.publishEvent(new MaintenanceRecordDeleted(record.getId(), record.getVehicleId()));
        recordRepository.delete(record);
        recordRepository.flush();
        currentMileageUpdater.refresh(vehicle);
    }

    private MaintenanceRecord find(UUID recordId) {
        return recordRepository.findById(recordId).orElseThrow(() -> accessGuard.missing(NOT_FOUND_MESSAGE));
    }

    private View view(MaintenanceRecord record, Vehicle vehicle) {
        RecordDocumentCounter counter = documentCounter.getIfAvailable();
        int documents = counter == null
                ? 0
                : counter.countByRecord(List.of(record.getId())).getOrDefault(record.getId(), 0);
        return new View(record, vehicle, authorPolicy.canEdit(record.getAuthorId()), documents);
    }

    /**
     * Valide la saisie puis l'applique. En modification ({@code existing}), un type désactivé
     * déjà présent sur l'entretien peut être conservé.
     */
    private void apply(MaintenanceRecordInput input, MaintenanceRecord record, Vehicle vehicle,
            MaintenanceRecord existing) {
        LocalDate date = input.getRecordDate();
        if (date.isAfter(today.date())) {
            throw ApiException.invalidField("recordDate", "La date de l'entretien ne peut pas être dans le futur.");
        }
        List<MaintenanceType> types = resolveTypes(input.getTypeIds(), existing);
        String description = Normalizers.blankToNull(input.getDescription());
        if (description == null) {
            types.stream().filter(MaintenanceType::isRequiresDescription).findFirst().ifPresent(type -> {
                throw ApiException.invalidField("description",
                        "Décrivez l'entretien réalisé (obligatoire pour « " + type.getName() + " »).");
            });
        }
        int km = input.getMileageKm();
        MileageConsistency.check(mileagePoints.of(vehicle.getId()), date, km,
                existing == null ? null : existing.getId(), vehicle.getInitialMileageKm(),
                Boolean.TRUE.equals(input.getConfirmLargeJump()));
        record.update(date, km, types, description, input.getCostCents(),
                Normalizers.blankToNull(input.getGarage()), Normalizers.blankToNull(input.getNotes()));
    }

    private List<MaintenanceType> resolveTypes(List<UUID> typeIds, MaintenanceRecord existing) {
        Set<UUID> unique = new HashSet<>(typeIds);
        if (unique.size() != typeIds.size()) {
            throw ApiException.invalidField("typeIds", "Chaque type d'entretien ne peut être choisi qu'une fois.");
        }
        Set<UUID> alreadyLinked = new HashSet<>();
        if (existing != null) {
            existing.getTypes().forEach(type -> alreadyLinked.add(type.getId()));
        }
        Map<UUID, MaintenanceType> found = new java.util.HashMap<>();
        typeRepository.findAllById(unique).forEach(type -> found.put(type.getId(), type));
        List<MaintenanceType> types = new ArrayList<>();
        for (UUID typeId : typeIds) {
            MaintenanceType type = found.get(typeId);
            if (type == null || type.getCategory() != MaintenanceTypeCategory.MAINTENANCE) {
                throw ApiException.invalidField("typeIds", "Ce type d'entretien n'existe pas.");
            }
            if (!type.isActive() && !alreadyLinked.contains(typeId)) {
                throw ApiException.invalidField("typeIds",
                        "Le type « " + type.getName() + " » n'est plus proposé. Choisissez-en un autre.");
            }
            types.add(type);
        }
        return types;
    }
}
