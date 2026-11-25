package fr.carsupervision.maintenance;

import fr.carsupervision.vehicle.VehicleMileageFloor;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Plus petit kilométrage saisi sur un véhicule (FR-009) ; les entretiens s'ajoutent en US2. */
@Component
class MileageFloorProvider implements VehicleMileageFloor {

    private final MileageReadingRepository readingRepository;

    MileageFloorProvider(MileageReadingRepository readingRepository) {
        this.readingRepository = readingRepository;
    }

    @Override
    public OptionalInt lowestRecordedMileage(UUID vehicleId) {
        return readingRepository.lowestMileage(vehicleId);
    }
}
