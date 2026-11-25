package fr.carsupervision.maintenance;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

/** Kilométrage daté d'un véhicule : un relevé ou, à partir de l'US2, un entretien (research R4). */
public record MileagePoint(UUID id, LocalDate date, int km) {

    /** Ordre chronologique : date, puis kilométrage (aucun ordre imposé entre points du même jour). */
    public static final Comparator<MileagePoint> CHRONOLOGICAL =
            Comparator.comparing(MileagePoint::date).thenComparingInt(MileagePoint::km);
}
