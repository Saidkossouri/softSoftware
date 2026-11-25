package fr.carsupervision.maintenance;

import fr.carsupervision.api.MileageApi;
import fr.carsupervision.api.model.MileageReadingInput;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Relevés kilométriques : droits et cohérence contrôlés par {@link MileageReadingService}. */
@RestController
public class MileageReadingsController implements MileageApi {

    private final MileageReadingService readingService;
    private final MileageReadingMapper readingMapper;

    public MileageReadingsController(MileageReadingService readingService, MileageReadingMapper readingMapper) {
        this.readingService = readingService;
        this.readingMapper = readingMapper;
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MileageReading> createMileageReading(UUID vehicleId,
            MileageReadingInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(readingService.create(vehicleId, input)));
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MileageReading> updateMileageReading(UUID readingId,
            MileageReadingInput input) {
        return ResponseEntity.ok(toDto(readingService.update(readingId, input)));
    }

    @Override
    public ResponseEntity<Void> deleteMileageReading(UUID readingId) {
        readingService.delete(readingId);
        return ResponseEntity.noContent().build();
    }

    private fr.carsupervision.api.model.MileageReading toDto(MileageReadingService.Saved saved) {
        return readingMapper.toDto(saved.reading(), saved.vehicle(), readingService.canEdit(saved.reading()));
    }
}
