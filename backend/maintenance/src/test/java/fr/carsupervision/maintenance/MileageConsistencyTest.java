package fr.carsupervision.maintenance;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MileageConsistencyTest {

    private static final int INITIAL = 50_000;
    private static final LocalDate D10 = LocalDate.of(2026, 11, 10);
    private static final LocalDate D20 = LocalDate.of(2026, 11, 20);
    private static final LocalDate D24 = LocalDate.of(2026, 11, 24);
    private static final MileagePoint P10 = new MileagePoint(UUID.randomUUID(), D10, 50_500);
    private static final MileagePoint P24 = new MileagePoint(UUID.randomUUID(), D24, 51_200);
    private static final List<MileagePoint> POINTS = List.of(P10, P24);

    private static void ok(LocalDate date, int km) {
        assertThatCode(() -> MileageConsistency.check(POINTS, date, km, null, INITIAL, false))
                .doesNotThrowAnyException();
    }

    private static void refused(LocalDate date, int km, ErrorCode code, String messagePart) {
        assertThatThrownBy(() -> MileageConsistency.check(POINTS, date, km, null, INITIAL, false))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    org.assertj.core.api.Assertions.assertThat(ex.getCode()).isEqualTo(code);
                    org.assertj.core.api.Assertions.assertThat(ex.getMessage()).contains(messagePart);
                });
    }

    @Test
    void acceptsValuesBetweenNeighboursBoundsIncluded() {
        ok(D20, 50_500);
        ok(D20, 51_200);
        ok(D20, 50_900);
        ok(D24.plusDays(1), 51_200);
    }

    @Test
    void refusesBelowInitialMileage() {
        refused(D10.minusDays(5), 49_999, ErrorCode.MILEAGE_INCONSISTENT, "50 000 km");
    }

    @Test
    void refusesLowerThanAnEarlierPointAndCitesIt() {
        refused(D24.plusDays(1), 50_900, ErrorCode.MILEAGE_INCONSISTENT, "51 200 km a été saisi le 24/11/2026");
    }

    @Test
    void refusesHigherThanALaterPointAndCitesIt() {
        refused(D20, 51_300, ErrorCode.MILEAGE_INCONSISTENT, "51 200 km a été saisi le 24/11/2026");
    }

    @Test
    void noConstraintBetweenPointsOfTheSameDay() {
        ok(D24, 51_000);
        ok(D24, 51_500);
    }

    @Test
    void ignoresThePointBeingEdited() {
        assertThatCode(() -> MileageConsistency.check(POINTS, D24.plusDays(1), 50_600, P24.id(), INITIAL, false))
                .doesNotThrowAnyException();
    }

    @Test
    void asksConfirmationAboveOneHundredThousandKilometres() {
        ok(D24.plusDays(1), 151_200);
        refused(D24.plusDays(1), 151_201, ErrorCode.MILEAGE_JUMP_NOT_CONFIRMED, "51 200 km le 24/11/2026");
        assertThatCode(() -> MileageConsistency.check(POINTS, D24.plusDays(1), 251_300, null, INITIAL, true))
                .doesNotThrowAnyException();
    }

    @Test
    void comparesJumpWithInitialMileageWhenThereIsNoEarlierPoint() {
        assertThatThrownBy(() -> MileageConsistency.check(List.of(), D10, 150_001, null, INITIAL, false))
                .isInstanceOfSatisfying(ApiException.class, ex -> org.assertj.core.api.Assertions
                        .assertThat(ex.getCode()).isEqualTo(ErrorCode.MILEAGE_JUMP_NOT_CONFIRMED));
    }
}
