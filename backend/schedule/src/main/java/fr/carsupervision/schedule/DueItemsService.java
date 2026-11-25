package fr.carsupervision.schedule;

import fr.carsupervision.common.time.Today;
import fr.carsupervision.maintenance.MaintenanceQueries;
import fr.carsupervision.maintenance.MaintenanceQueries.Occurrence;
import fr.carsupervision.schedule.DueDateCalculator.Due;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Échéances calculées d'un véhicule (FR-023 à FR-027, research R10) : règles et échéances à date
 * fixe à faire, jamais stockées, recalculées à chaque lecture.
 */
@Service
public class DueItemsService {

    private final MaintenanceRuleRepository ruleRepository;
    private final DeadlineRepository deadlineRepository;
    private final MaintenanceQueries maintenanceQueries;
    private final VehicleService vehicleService;
    private final Today today;

    public DueItemsService(MaintenanceRuleRepository ruleRepository, DeadlineRepository deadlineRepository,
            MaintenanceQueries maintenanceQueries, VehicleService vehicleService, Today today) {
        this.ruleRepository = ruleRepository;
        this.deadlineRepository = deadlineRepository;
        this.maintenanceQueries = maintenanceQueries;
        this.vehicleService = vehicleService;
        this.today = today;
    }

    /**
     * Une échéance : règle ({@code rule}, dernière réalisation, entretien de référence éventuel)
     * ou échéance à date fixe ({@code deadline}).
     */
    public record Item(Due due, MaintenanceRule rule, LocalDate lastDate, Integer lastKm, UUID lastRecordId,
            Deadline deadline) {

        String name() {
            return rule != null ? rule.getType().getName() : deadline.getLabel();
        }
    }

    static final Comparator<Item> ORDER = Comparator.comparing(Item::due, DueDateCalculator.ORDER)
            .thenComparing(Item::name, String.CASE_INSENSITIVE_ORDER);

    /** Mêmes droits que la lecture du véhicule (un ADMIN lit aussi un véhicule archivé). */
    @Transactional(readOnly = true)
    public List<Item> forVehicle(UUID vehicleId) {
        return items(vehicleService.get(vehicleId));
    }

    /** Sans contrôle d'accès : l'appelant a déjà filtré les véhicules. */
    @Transactional(readOnly = true)
    public List<Item> items(Vehicle vehicle) {
        LocalDate now = today.date();
        List<Item> items = new ArrayList<>();
        for (MaintenanceRule rule : ruleRepository.findByVehicleId(vehicle.getId())) {
            items.add(ruleItem(rule, vehicle, now));
        }
        for (Deadline deadline : deadlineRepository.findByVehicleIdAndDoneAtIsNull(vehicle.getId())) {
            items.add(new Item(DueDateCalculator.forDate(deadline.getDueDate(), now), null, null, null, null,
                    deadline));
        }
        items.sort(ORDER);
        return items;
    }

    /**
     * Dernière réalisation : l'entretien le plus récent comportant le type s'il n'est pas antérieur
     * au point de départ de la règle, sinon le point de départ.
     */
    private Item ruleItem(MaintenanceRule rule, Vehicle vehicle, LocalDate now) {
        Occurrence last = maintenanceQueries.lastOccurrence(vehicle.getId(), rule.getType().getId())
                .filter(occurrence -> !occurrence.date().isBefore(rule.getBaselineDate()))
                .orElse(null);
        LocalDate lastDate = last != null ? last.date() : rule.getBaselineDate();
        int lastKm = last != null ? last.mileageKm() : rule.getBaselineMileageKm();
        Due due = DueDateCalculator.forRule(lastDate, lastKm, rule.getIntervalKm(), rule.getIntervalMonths(),
                vehicle.getCurrentMileageKm(), now);
        return new Item(due, rule, lastDate, lastKm, last != null ? last.recordId() : null, null);
    }
}
