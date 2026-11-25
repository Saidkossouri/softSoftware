package fr.carsupervision.maintenance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Tous les points kilométriques d'un véhicule : relevés et entretiens (research R4). */
@Component
public class MileagePoints {

    private final MileageReadingRepository readingRepository;
    private final MaintenanceRecordRepository recordRepository;

    public MileagePoints(MileageReadingRepository readingRepository, MaintenanceRecordRepository recordRepository) {
        this.readingRepository = readingRepository;
        this.recordRepository = recordRepository;
    }

    public List<MileagePoint> of(UUID vehicleId) {
        List<MileagePoint> points = new ArrayList<>(readingRepository.findPoints(vehicleId));
        points.addAll(recordRepository.findPoints(vehicleId));
        return points;
    }
}
