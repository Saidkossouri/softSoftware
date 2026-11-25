package fr.carsupervision.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Mise en forme des valeurs citées dans les messages d'erreur (« 48 000 km », « 24/11/2026 »). */
public final class Formats {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formats() {
    }

    public static String km(int kilometers) {
        return String.format(Locale.ROOT, "%,d", kilometers).replace(',', ' ') + " km";
    }

    public static String date(LocalDate date) {
        return DATE.format(date);
    }
}
