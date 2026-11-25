package fr.carsupervision.schedule;

import fr.carsupervision.common.time.Today;
import fr.carsupervision.schedule.DueDateCalculator.State;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleAccessGuard;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rappels de l'accueil (FR-028, FR-035, research R11) pour les véhicules actifs accessibles :
 * échéances en retard ou proches, et véhicules sans kilométrage saisi depuis plus de 30 jours.
 */
@Service
public class RemindersService {

    static final int MILEAGE_REMINDER_DAYS = 30;

    private final VehicleAccessGuard accessGuard;
    private final DueItemsService dueItemsService;
    private final Today today;

    public RemindersService(VehicleAccessGuard accessGuard, DueItemsService dueItemsService, Today today) {
        this.accessGuard = accessGuard;
        this.dueItemsService = dueItemsService;
        this.today = today;
    }

    public record VehicleItem(Vehicle vehicle, DueItemsService.Item item) {
    }

    public record Reminders(List<VehicleItem> dueItems, List<Vehicle> mileageReadings) {
    }

    @Transactional(readOnly = true)
    public Reminders get() {
        LocalDate limit = today.date().minusDays(MILEAGE_REMINDER_DAYS);
        List<VehicleItem> dueItems = new ArrayList<>();
        List<Vehicle> mileage = new ArrayList<>();
        for (Vehicle vehicle : accessGuard.activeAccessibleVehicles()) {
            dueItemsService.items(vehicle).stream()
                    .filter(item -> item.due().state() != State.OK)
                    .forEach(item -> dueItems.add(new VehicleItem(vehicle, item)));
            if (vehicle.getCurrentMileageDate().isBefore(limit)) {
                mileage.add(vehicle);
            }
        }
        dueItems.sort(Comparator.comparing(VehicleItem::item, DueItemsService.ORDER)
                .thenComparing(entry -> entry.vehicle().getNickname()));
        mileage.sort(Comparator.comparing(Vehicle::getCurrentMileageDate).thenComparing(Vehicle::getNickname));
        return new Reminders(dueItems, mileage);
    }
}
