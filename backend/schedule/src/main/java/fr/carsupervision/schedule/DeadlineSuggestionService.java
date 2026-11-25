package fr.carsupervision.schedule;

import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.time.Today;
import fr.carsupervision.maintenance.MaintenanceType;
import fr.carsupervision.maintenance.MaintenanceTypeCategory;
import fr.carsupervision.maintenance.MaintenanceTypeRepository;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleAccessGuard;
import fr.carsupervision.vehicle.VehicleService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suggestions d'échéances administratives selon l'âge du véhicule (FR-040) : au Maroc,
 * assurance et vignette pour tout véhicule, visite technique à partir de 5 ans (catalogue).
 */
@Service
public class DeadlineSuggestionService {

    private final MaintenanceTypeRepository typeRepository;
    private final DeadlineRepository deadlineRepository;
    private final DeadlineSuggestionDismissalRepository dismissalRepository;
    private final VehicleService vehicleService;
    private final VehicleAccessGuard accessGuard;
    private final Today today;
    private final Clock clock;

    public DeadlineSuggestionService(MaintenanceTypeRepository typeRepository, DeadlineRepository deadlineRepository,
            DeadlineSuggestionDismissalRepository dismissalRepository, VehicleService vehicleService,
            VehicleAccessGuard accessGuard, Today today, Clock clock) {
        this.typeRepository = typeRepository;
        this.deadlineRepository = deadlineRepository;
        this.dismissalRepository = dismissalRepository;
        this.vehicleService = vehicleService;
        this.accessGuard = accessGuard;
        this.today = today;
        this.clock = clock;
    }

    /**
     * Types Administratif actifs dont l'âge de suggestion est atteint (âge 0 : tout véhicule, même
     * sans année), sans échéance à faire de ce type, non ignorés ; vide pour un véhicule archivé.
     */
    @Transactional(readOnly = true)
    public List<MaintenanceType> list(UUID vehicleId) {
        Vehicle vehicle = vehicleService.get(vehicleId);
        if (!vehicle.isActive()) {
            return List.of();
        }
        Integer year = vehicle.getFirstRegistrationYear();
        int age = year == null ? 0 : Math.max(0, today.year() - year);
        Set<UUID> excluded = new HashSet<>(dismissalRepository.findDismissedTypeIds(vehicleId));
        deadlineRepository.findByVehicleIdAndDoneAtIsNull(vehicleId).stream()
                .filter(deadline -> deadline.getType() != null)
                .forEach(deadline -> excluded.add(deadline.getType().getId()));
        return typeRepository.findAllByOrderByCategoryAscDisplayOrderAscNameAsc().stream()
                .filter(type -> type.getCategory() == MaintenanceTypeCategory.ADMINISTRATIVE && type.isActive())
                .filter(type -> type.getSuggestFromAgeYears() != null && type.getSuggestFromAgeYears() <= age)
                .filter(type -> !excluded.contains(type.getId()))
                .toList();
    }

    /** Idempotent ; 409 si le véhicule est archivé. */
    @Transactional
    public void dismiss(UUID vehicleId, UUID typeId) {
        accessGuard.lockForWrite(vehicleId);
        typeRepository.findById(typeId)
                .filter(type -> type.getCategory() == MaintenanceTypeCategory.ADMINISTRATIVE)
                .orElseThrow(() -> ApiException.notFound("Ce type d'échéance n'existe pas."));
        var key = new DeadlineSuggestionDismissal.Key(vehicleId, typeId);
        if (!dismissalRepository.existsById(key)) {
            dismissalRepository.save(new DeadlineSuggestionDismissal(key, OffsetDateTime.now(clock)));
        }
    }
}
