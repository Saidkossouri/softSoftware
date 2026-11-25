package fr.carsupervision.maintenance;

import fr.carsupervision.api.model.MileageReadingInput;
import fr.carsupervision.common.Normalizers;
import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.time.Today;
import fr.carsupervision.vehicle.AuthorPolicy;
import fr.carsupervision.vehicle.AuthorPolicy.AuthorSnapshot;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleAccessGuard;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relevés kilométriques (US1, FR-005 à FR-008). Chaque écriture verrouille d'abord le véhicule
 * (droits, véhicule archivé, sérialisation des saisies), puis contrôle l'auteur, la saisie et la
 * cohérence chronologique, et recalcule enfin le kilométrage actuel.
 */
@Service
public class MileageReadingService {

    static final String NOT_FOUND_MESSAGE = "Ce relevé n'existe pas.";
    private static final int NOTE_MAX_LENGTH = 200;

    private final MileageReadingRepository readingRepository;
    private final MileagePoints mileagePoints;
    private final CurrentMileageUpdater currentMileageUpdater;
    private final VehicleAccessGuard accessGuard;
    private final AuthorPolicy authorPolicy;
    private final Today today;
    private final Clock clock;

    public MileageReadingService(MileageReadingRepository readingRepository, MileagePoints mileagePoints,
            CurrentMileageUpdater currentMileageUpdater, VehicleAccessGuard accessGuard, AuthorPolicy authorPolicy,
            Today today, Clock clock) {
        this.readingRepository = readingRepository;
        this.mileagePoints = mileagePoints;
        this.currentMileageUpdater = currentMileageUpdater;
        this.accessGuard = accessGuard;
        this.authorPolicy = authorPolicy;
        this.today = today;
        this.clock = clock;
    }

    /** Relevé enregistré et véhicule (kilométrage actuel à jour). */
    public record Saved(MileageReading reading, Vehicle vehicle) {
    }

    @Transactional
    public Saved create(UUID vehicleId, MileageReadingInput input) {
        Vehicle vehicle = accessGuard.lockForWrite(vehicleId);
        AuthorSnapshot author = authorPolicy.authorSnapshot();
        MileageReading reading = new MileageReading(vehicleId, author.id(), author.name(), OffsetDateTime.now(clock));
        apply(input, reading, vehicle, null);
        readingRepository.saveAndFlush(reading);
        currentMileageUpdater.refresh(vehicle);
        return new Saved(reading, vehicle);
    }

    @Transactional
    public Saved update(UUID readingId, MileageReadingInput input) {
        MileageReading reading = find(readingId);
        Vehicle vehicle = accessGuard.lockForWrite(reading.getVehicleId());
        authorPolicy.assertAuthorOrAdmin(reading.getAuthorId());
        apply(input, reading, vehicle, reading.getId());
        reading.setUpdatedAt(OffsetDateTime.now(clock));
        readingRepository.saveAndFlush(reading);
        currentMileageUpdater.refresh(vehicle);
        return new Saved(reading, vehicle);
    }

    /** Une suppression ne peut pas créer d'incohérence ; le kilométrage actuel est recalculé. */
    @Transactional
    public void delete(UUID readingId) {
        MileageReading reading = find(readingId);
        Vehicle vehicle = accessGuard.lockForWrite(reading.getVehicleId());
        authorPolicy.assertAuthorOrAdmin(reading.getAuthorId());
        readingRepository.delete(reading);
        readingRepository.flush();
        currentMileageUpdater.refresh(vehicle);
    }

    public boolean canEdit(MileageReading reading) {
        return authorPolicy.canEdit(reading.getAuthorId());
    }

    private MileageReading find(UUID readingId) {
        return readingRepository.findById(readingId).orElseThrow(() -> accessGuard.missing(NOT_FOUND_MESSAGE));
    }

    private void apply(MileageReadingInput input, MileageReading reading, Vehicle vehicle, UUID excludedId) {
        LocalDate date = input.getReadingDate();
        if (date.isAfter(today.date())) {
            throw ApiException.invalidField("readingDate", "La date du relevé ne peut pas être dans le futur.");
        }
        String note = Normalizers.blankToNull(input.getNote());
        if (note != null && note.length() > NOTE_MAX_LENGTH) {
            throw ApiException.invalidField("note", "200 caractères au maximum.");
        }
        int km = input.getMileageKm();
        MileageConsistency.check(mileagePoints.of(vehicle.getId()), date, km, excludedId,
                vehicle.getInitialMileageKm(), Boolean.TRUE.equals(input.getConfirmLargeJump()));
        reading.update(date, km, note);
    }
}
