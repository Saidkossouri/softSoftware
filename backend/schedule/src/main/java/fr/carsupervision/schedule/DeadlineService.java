package fr.carsupervision.schedule;

import fr.carsupervision.api.model.DeadlineInput;
import fr.carsupervision.common.Normalizers;
import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.maintenance.MaintenanceType;
import fr.carsupervision.maintenance.MaintenanceTypeCategory;
import fr.carsupervision.maintenance.MaintenanceTypeRepository;
import fr.carsupervision.vehicle.VehicleAccessGuard;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Échéances à date fixe (FR-036 à FR-039) : tout membre ayant accès au véhicule les gère. */
@Service
public class DeadlineService {

    static final String NOT_FOUND_MESSAGE = "Cette échéance n'existe pas.";

    private final DeadlineRepository deadlineRepository;
    private final MaintenanceTypeRepository typeRepository;
    private final VehicleAccessGuard accessGuard;
    private final Clock clock;

    public DeadlineService(DeadlineRepository deadlineRepository, MaintenanceTypeRepository typeRepository,
            VehicleAccessGuard accessGuard, Clock clock) {
        this.deadlineRepository = deadlineRepository;
        this.typeRepository = typeRepository;
        this.accessGuard = accessGuard;
        this.clock = clock;
    }

    /** Échéance faite et, si elle est récurrente, la suivante. */
    public record Completion(Deadline completed, Deadline next) {
    }

    @Transactional
    public Deadline create(UUID vehicleId, DeadlineInput input) {
        accessGuard.lockForWrite(vehicleId);
        Deadline deadline = new Deadline(vehicleId, OffsetDateTime.now(clock));
        apply(deadline, input, null);
        return deadlineRepository.saveAndFlush(deadline);
    }

    @Transactional
    public Deadline update(UUID deadlineId, DeadlineInput input) {
        Deadline deadline = find(deadlineId);
        accessGuard.lockForWrite(deadline.getVehicleId());
        apply(deadline, input, deadline.getType());
        return deadlineRepository.saveAndFlush(deadline);
    }

    @Transactional
    public void delete(UUID deadlineId) {
        Deadline deadline = find(deadlineId);
        accessGuard.lockForWrite(deadline.getVehicleId());
        deadlineRepository.delete(deadline);
    }

    /**
     * Idempotent : une échéance déjà faite est renvoyée telle quelle. La suivante part de la date
     * prévue, pas du jour où on la coche (l'assurance du 1er avril reste au 1er avril).
     */
    @Transactional
    public Completion complete(UUID deadlineId) {
        Deadline deadline = find(deadlineId);
        accessGuard.lockForWrite(deadline.getVehicleId());
        if (deadline.isDone()) {
            return new Completion(deadline, null);
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        deadline.markDone(now);
        Deadline next = deadline.getRecurrenceMonths() == null ? null : deadlineRepository.save(deadline.next(now));
        deadlineRepository.flush();
        return new Completion(deadline, next);
    }

    private Deadline find(UUID deadlineId) {
        return deadlineRepository.findById(deadlineId).orElseThrow(() -> accessGuard.missing(NOT_FOUND_MESSAGE));
    }

    /** En modification, le type déjà choisi reste accepté même s'il a été désactivé depuis. */
    private void apply(Deadline deadline, DeadlineInput input, MaintenanceType current) {
        MaintenanceType type = null;
        if (input.getTypeId() != null) {
            type = typeRepository.findById(input.getTypeId())
                    .filter(found -> found.getCategory() == MaintenanceTypeCategory.ADMINISTRATIVE)
                    .orElseThrow(() -> ApiException.invalidField("typeId", "Ce type d'échéance n'existe pas."));
            boolean kept = current != null && current.getId().equals(type.getId());
            if (!type.isActive() && !kept) {
                throw ApiException.invalidField("typeId",
                        "Le type « " + type.getName() + " » n'est plus proposé. Choisissez-en un autre.");
            }
        }
        String label = Normalizers.blankToNull(input.getLabel());
        if (label == null && type == null) {
            throw ApiException.invalidField("label", "Choisissez un type d'échéance ou saisissez un libellé.");
        }
        deadline.update(type, label != null ? label : type.getName(), input.getDueDate(),
                input.getRecurrenceMonths(), OffsetDateTime.now(clock));
    }
}
