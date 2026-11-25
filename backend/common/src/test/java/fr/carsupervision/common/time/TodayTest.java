package fr.carsupervision.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TodayTest {

    private static Today at(String instant) {
        return new Today(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), "Africa/Casablanca");
    }

    @Test
    void passeAuLendemainAMinuitAuMarocQuandLHorlogeUtcEstEncoreLaVeille() {
        // En juin 2026, le Maroc est à UTC+1 : 23 h 30 UTC = 0 h 30 le lendemain.
        Today today = at("2026-06-15T23:30:00Z");

        assertThat(today.date()).isEqualTo(LocalDate.of(2026, 6, 16));
    }

    @Test
    void donneLAnneeDuJourAuMaroc() {
        // La base tzdata (2026f) fait revenir le Maroc à UTC+0 le 20 septembre 2026 : le fuseau
        // suit ces changements sans modification du code.
        Today today = at("2026-12-31T23:30:00Z");

        assertThat(today.date()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(today.year()).isEqualTo(2026);
    }

    @Test
    void resteLeJourMemePendantLeRamadanOuLeMarocEstAUtc() {
        // Pendant le ramadan 2026 (mi-février à mi-mars), le Maroc revient à UTC+0.
        Today today = at("2026-03-05T23:30:00Z");

        assertThat(today.date()).isEqualTo(LocalDate.of(2026, 3, 5));
    }
}
