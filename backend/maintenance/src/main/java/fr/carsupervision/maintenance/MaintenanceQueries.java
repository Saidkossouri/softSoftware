package fr.carsupervision.maintenance;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lectures des entretiens pour les modules au-dessus (échéances), sans contrôle d'accès. */
@Service
public class MaintenanceQueries {

    private final MaintenanceRecordRepository recordRepository;

    public MaintenanceQueries(MaintenanceRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /** Une réalisation : l'entretien, sa date et son kilométrage. */
    public record Occurrence(UUID recordId, LocalDate date, int mileageKm) {
    }

    /** Entretien le plus récent du véhicule comportant ce type (date, puis kilométrage). */
    @Transactional(readOnly = true)
    public Optional<Occurrence> lastOccurrence(UUID vehicleId, UUID typeId) {
        return recordRepository.findOccurrences(vehicleId, typeId, Limit.of(1)).stream()
                .findFirst()
                .map(point -> new Occurrence(point.id(), point.date(), point.km()));
    }
}
