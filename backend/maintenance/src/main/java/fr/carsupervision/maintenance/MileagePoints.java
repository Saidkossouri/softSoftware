package fr.carsupervision.maintenance;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Tous les points kilométriques d'un véhicule (relevés ; les entretiens s'ajoutent en US2). */
@Component
public class MileagePoints {

    private final MileageReadingRepository readingRepository;

    public MileagePoints(MileageReadingRepository readingRepository) {
        this.readingRepository = readingRepository;
    }

    public List<MileagePoint> of(UUID vehicleId) {
        return readingRepository.findPoints(vehicleId);
    }
}
