package fr.carsupervision.maintenance;

import fr.carsupervision.vehicle.VehicleMileageFloor;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** Plus petit kilométrage saisi sur un véhicule, relevés et entretiens confondus (FR-009). */
@Component
class MileageFloorProvider implements VehicleMileageFloor {

    private final MileageReadingRepository readingRepository;
    private final MaintenanceRecordRepository recordRepository;

    MileageFloorProvider(MileageReadingRepository readingRepository, MaintenanceRecordRepository recordRepository) {
        this.readingRepository = readingRepository;
        this.recordRepository = recordRepository;
    }

    @Override
    public OptionalInt lowestRecordedMileage(UUID vehicleId) {
        return Stream.of(readingRepository.findLowestMileage(vehicleId), recordRepository.findLowestMileage(vehicleId))
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .min();
    }
}
