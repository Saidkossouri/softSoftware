package fr.carsupervision.schedule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

/**
 * Calcul pur des échéances (research R10, principe V) : aucune dépendance Spring, jamais stocké.
 * Un terme est en retard quand son reste est nul ou négatif, proche sous 30 jours ou 1 000 km ;
 * avec deux termes, le plus défavorable l'emporte.
 */
public final class DueDateCalculator {

    public static final int SOON_DAYS = 30;
    public static final int SOON_KM = 1_000;

    public enum State {
        OVERDUE, SOON, OK
    }

    /** Échéance calculée ; les champs d'un terme absent sont nuls. Restes négatifs en cas de retard. */
    public record Due(LocalDate dueDate, Integer dueMileageKm, Integer remainingDays, Integer remainingKm,
            State state) {
    }

    /** En retard d'abord, puis proche, puis à jour ; à état égal, la plus proche d'abord. */
    public static final Comparator<Due> ORDER = Comparator.comparing(Due::state)
            .thenComparingDouble(DueDateCalculator::proximity);

    private DueDateCalculator() {
    }

    /**
     * Échéance d'une règle à partir de sa dernière réalisation. Au moins un intervalle est
     * renseigné ; {@code plusMonths} ramène une fin de mois au dernier jour (31/01 + 1 mois = 28/02).
     */
    public static Due forRule(LocalDate lastDate, int lastKm, Integer intervalKm, Integer intervalMonths,
            int currentKm, LocalDate today) {
        if (intervalKm == null && intervalMonths == null) {
            throw new IllegalArgumentException("Une règle a au moins un intervalle.");
        }
        LocalDate dueDate = intervalMonths == null ? null : lastDate.plusMonths(intervalMonths);
        Integer dueKm = intervalKm == null ? null : lastKm + intervalKm;
        Integer remainingDays = dueDate == null ? null : days(today, dueDate);
        Integer remainingKm = dueKm == null ? null : dueKm - currentKm;
        State state = worst(remainingDays == null ? State.OK : byDays(remainingDays),
                remainingKm == null ? State.OK : byKm(remainingKm));
        return new Due(dueDate, dueKm, remainingDays, remainingKm, state);
    }

    /** Échéance à date fixe : même règle sur la seule date. */
    public static Due forDate(LocalDate dueDate, LocalDate today) {
        int remainingDays = days(today, dueDate);
        return new Due(dueDate, null, remainingDays, null, byDays(remainingDays));
    }

    static State byDays(int remainingDays) {
        return remainingDays <= 0 ? State.OVERDUE : remainingDays < SOON_DAYS ? State.SOON : State.OK;
    }

    static State byKm(int remainingKm) {
        return remainingKm <= 0 ? State.OVERDUE : remainingKm < SOON_KM ? State.SOON : State.OK;
    }

    private static State worst(State a, State b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static int days(LocalDate from, LocalDate to) {
        return Math.toIntExact(ChronoUnit.DAYS.between(from, to));
    }

    /** Proximité commune aux deux termes : le reste rapporté à son seuil (30 jours, 1 000 km). */
    private static double proximity(Due due) {
        double days = due.remainingDays() == null ? Double.MAX_VALUE : due.remainingDays() / (double) SOON_DAYS;
        double km = due.remainingKm() == null ? Double.MAX_VALUE : due.remainingKm() / (double) SOON_KM;
        return Math.min(days, km);
    }
}
