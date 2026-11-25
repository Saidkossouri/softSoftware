package fr.carsupervision.common.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Date du jour dans le fuseau de l'application ({@code app.time-zone}, par défaut
 * {@code Africa/Casablanca}) à partir de l'horloge partagée, qui est en UTC (research R8).
 * Sert aux contrôles « pas dans le futur », à l'année des totaux et au calcul des échéances.
 */
@Component
public class Today {

    private final Clock clock;
    private final ZoneId zone;

    public Today(Clock clock, @Value("${app.time-zone:Africa/Casablanca}") String zone) {
        this.clock = clock;
        this.zone = ZoneId.of(zone);
    }

    public LocalDate date() {
        return LocalDate.now(clock.withZone(zone));
    }

    public int year() {
        return date().getYear();
    }

    public ZoneId zone() {
        return zone;
    }
}
