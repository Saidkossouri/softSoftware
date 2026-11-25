package fr.carsupervision.maintenance;

import fr.carsupervision.api.model.FieldError;
import fr.carsupervision.common.Formats;
import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/**
 * Cohérence chronologique d'un kilométrage saisi (FR-006, FR-007, research R4). Classe pure :
 * <ul>
 *   <li>au moins le kilométrage de départ du véhicule ;</li>
 *   <li>au moins le plus grand kilométrage des points datés avant ;</li>
 *   <li>au plus le plus petit kilométrage des points datés après ;</li>
 *   <li>aucune contrainte entre points du même jour ;</li>
 *   <li>plus de 100 000 km au-dessus du point précédent : confirmation demandée.</li>
 * </ul>
 */
public final class MileageConsistency {

    static final int LARGE_JUMP_KM = 100_000;

    private MileageConsistency() {
    }

    /**
     * @param excludedId point en cours de modification, ignoré (ou {@code null} à la création)
     * @throws ApiException 409 {@code MILEAGE_INCONSISTENT} ou {@code MILEAGE_JUMP_NOT_CONFIRMED}
     */
    public static void check(Collection<MileagePoint> points, LocalDate date, int km, UUID excludedId,
            int initialKm, boolean confirmLargeJump) {
        if (km < initialKm) {
            throw inconsistent("Le kilométrage ne peut pas être inférieur au kilométrage de départ du véhicule ("
                    + Formats.km(initialKm) + ").");
        }
        List<MileagePoint> others = points.stream()
                .filter(point -> excludedId == null || !excludedId.equals(point.id()))
                .toList();

        Optional<MileagePoint> highestBefore = others.stream()
                .filter(point -> point.date().isBefore(date))
                .max(java.util.Comparator.comparingInt(MileagePoint::km));
        if (highestBefore.isPresent() && km < highestBefore.get().km()) {
            MileagePoint blocking = highestBefore.get();
            throw inconsistent("Un kilométrage de " + Formats.km(blocking.km()) + " a été saisi le "
                    + Formats.date(blocking.date()) + " : le kilométrage ne peut pas être plus petit à une date"
                    + " postérieure.");
        }

        Optional<MileagePoint> lowestAfter = others.stream()
                .filter(point -> point.date().isAfter(date))
                .min(java.util.Comparator.comparingInt(MileagePoint::km));
        if (lowestAfter.isPresent() && km > lowestAfter.get().km()) {
            MileagePoint blocking = lowestAfter.get();
            throw inconsistent("Un kilométrage de " + Formats.km(blocking.km()) + " a été saisi le "
                    + Formats.date(blocking.date()) + " : le kilométrage ne peut pas être plus grand à une date"
                    + " antérieure.");
        }

        if (!confirmLargeJump) {
            Optional<MileagePoint> previous = others.stream()
                    .filter(point -> !point.date().isAfter(date))
                    .max(MileagePoint.CHRONOLOGICAL);
            int previousKm = previous.map(MileagePoint::km).orElse(initialKm);
            if (km - previousKm > LARGE_JUMP_KM) {
                String reference = previous
                        .map(point -> Formats.km(point.km()) + " le " + Formats.date(point.date()))
                        .orElse(Formats.km(initialKm) + " au départ");
                throw new ApiException(HttpStatus.CONFLICT, ErrorCode.MILEAGE_JUMP_NOT_CONFIRMED,
                        "Ce kilométrage dépasse de plus de 100 000 km le précédent (" + reference
                                + "). Confirmez-le s'il est exact.");
            }
        }
    }

    private static ApiException inconsistent(String message) {
        return new ApiException(HttpStatus.CONFLICT, ErrorCode.MILEAGE_INCONSISTENT, message,
                List.of(new FieldError("mileageKm", message)));
    }
}
