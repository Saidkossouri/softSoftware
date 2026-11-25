package fr.carsupervision.maintenance;

import fr.carsupervision.common.time.Today;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleService;
import org.springframework.stereotype.Component;

/**
 * Recalcule le kilométrage actuel stocké sur le véhicule (research R3) après chaque écriture :
 * le point le plus récent (date, puis kilométrage), sinon le kilométrage de départ à la date de
 * création du véhicule.
 */
@Component
public class CurrentMileageUpdater {

    private final MileagePoints mileagePoints;
    private final VehicleService vehicleService;
    private final Today today;

    public CurrentMileageUpdater(MileagePoints mileagePoints, VehicleService vehicleService, Today today) {
        this.mileagePoints = mileagePoints;
        this.vehicleService = vehicleService;
        this.today = today;
    }

    /** À appeler dans la transaction qui a verrouillé le véhicule. */
    public void refresh(Vehicle vehicle) {
        mileagePoints.of(vehicle.getId()).stream()
                .max(MileagePoint.CHRONOLOGICAL)
                .ifPresentOrElse(
                        latest -> vehicleService.updateCurrentMileage(vehicle.getId(), latest.km(), latest.date()),
                        () -> vehicleService.updateCurrentMileage(vehicle.getId(), vehicle.getInitialMileageKm(),
                                vehicle.getCreatedAt().atZoneSameInstant(today.zone()).toLocalDate()));
    }
}
