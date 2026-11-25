package fr.carsupervision.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Horloge des tests d'intégration : fixée au {@link #DEFAULT} au début de chaque test, déplaçable
 * par un test (date « du jour », rappel de 30 jours, échéances).
 */
public class MutableClock extends Clock {

    /** 15 juin 2026 à 10 h UTC, soit 11 h au Maroc (UTC+1). */
    public static final Instant DEFAULT = Instant.parse("2026-06-15T10:00:00Z");

    private volatile Instant instant = DEFAULT;

    public void set(Instant instant) {
        this.instant = instant;
    }

    public void advance(Duration duration) {
        this.instant = instant.plus(duration);
    }

    public void reset() {
        this.instant = DEFAULT;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        // Vue sur la même horloge mobile dans un autre fuseau (utilisée par Today).
        MutableClock parent = this;
        return new Clock() {
            @Override
            public ZoneId getZone() {
                return zone;
            }

            @Override
            public Clock withZone(ZoneId other) {
                return parent.withZone(other);
            }

            @Override
            public Instant instant() {
                return parent.instant();
            }
        };
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
