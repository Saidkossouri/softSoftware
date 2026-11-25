package fr.carsupervision.schedule;

import static fr.carsupervision.schedule.DueDateCalculator.State.OK;
import static fr.carsupervision.schedule.DueDateCalculator.State.OVERDUE;
import static fr.carsupervision.schedule.DueDateCalculator.State.SOON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.carsupervision.schedule.DueDateCalculator.Due;
import fr.carsupervision.schedule.DueDateCalculator.State;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Table de cas du calcul des échéances (research R10, principe V). */
class DueDateCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Test
    void dateOnlyRule() {
        Due due = DueDateCalculator.forRule(LocalDate.of(2025, 9, 15), 45_000, null, 12, 59_200, TODAY);

        assertThat(due.dueDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(due.dueMileageKm()).isNull();
        assertThat(due.remainingDays()).isEqualTo(92);
        assertThat(due.remainingKm()).isNull();
        assertThat(due.state()).isEqualTo(OK);
    }

    @Test
    void mileageOnlyRule() {
        Due due = DueDateCalculator.forRule(LocalDate.of(2025, 9, 15), 45_000, 15_000, null, 59_200, TODAY);

        assertThat(due.dueDate()).isNull();
        assertThat(due.dueMileageKm()).isEqualTo(60_000);
        assertThat(due.remainingKm()).isEqualTo(800);
        assertThat(due.state()).isEqualTo(SOON);
    }

    /** Scénario US4-1 : dernière vidange à 45 000 km il y a 9 mois, 59 200 km, « 15 000 km ou 12 mois ». */
    @Test
    void bothTermsTheWorstWins() {
        Due soon = DueDateCalculator.forRule(LocalDate.of(2025, 9, 15), 45_000, 15_000, 12, 59_200, TODAY);
        assertThat(soon.remainingDays()).isEqualTo(92);
        assertThat(soon.remainingKm()).isEqualTo(800);
        assertThat(soon.state()).isEqualTo(SOON);

        Due overdue = DueDateCalculator.forRule(LocalDate.of(2025, 9, 15), 45_000, 15_000, 12, 60_400, TODAY);
        assertThat(overdue.remainingKm()).isEqualTo(-400);
        assertThat(overdue.state()).isEqualTo(OVERDUE);

        Due byDate = DueDateCalculator.forRule(LocalDate.of(2025, 6, 1), 45_000, 15_000, 12, 46_000, TODAY);
        assertThat(byDate.remainingDays()).isEqualTo(-14);
        assertThat(byDate.remainingKm()).isEqualTo(14_000);
        assertThat(byDate.state()).isEqualTo(OVERDUE);
    }

    @ParameterizedTest
    @CsvSource({ "30, OK", "29, SOON", "1, SOON", "0, OVERDUE", "-5, OVERDUE" })
    void daysThresholds(int remainingDays, State expected) {
        Due due = DueDateCalculator.forDate(TODAY.plusDays(remainingDays), TODAY);

        assertThat(due.remainingDays()).isEqualTo(remainingDays);
        assertThat(due.state()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({ "1000, OK", "999, SOON", "1, SOON", "0, OVERDUE", "-250, OVERDUE" })
    void kmThresholds(int remainingKm, State expected) {
        Due due = DueDateCalculator.forRule(TODAY, 50_000, 10_000, null, 60_000 - remainingKm, TODAY);

        assertThat(due.remainingKm()).isEqualTo(remainingKm);
        assertThat(due.state()).isEqualTo(expected);
    }

    @Test
    void endOfMonthIsClampedToLastDay() {
        assertThat(DueDateCalculator.forRule(LocalDate.of(2027, 1, 31), 0, null, 1, 0, TODAY).dueDate())
                .isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(DueDateCalculator.forRule(LocalDate.of(2028, 1, 31), 0, null, 1, 0, TODAY).dueDate())
                .isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void ruleWithoutIntervalIsRejected() {
        assertThatThrownBy(() -> DueDateCalculator.forRule(TODAY, 0, null, null, 0, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sortsOverdueThenSoonThenOkThenByProximity() {
        Due okFar = DueDateCalculator.forDate(TODAY.plusDays(200), TODAY);
        Due okNear = DueDateCalculator.forDate(TODAY.plusDays(40), TODAY);
        Due soonKm = DueDateCalculator.forRule(TODAY, 50_000, 10_000, null, 59_900, TODAY); // 100 km
        Due soonDays = DueDateCalculator.forDate(TODAY.plusDays(20), TODAY);
        Due overdueLittle = DueDateCalculator.forDate(TODAY.minusDays(1), TODAY);
        Due overdueMuch = DueDateCalculator.forRule(TODAY, 50_000, 10_000, null, 62_000, TODAY); // -2 000 km

        List<Due> sorted = new ArrayList<>(List.of(okFar, soonDays, overdueLittle, okNear, soonKm, overdueMuch));
        sorted.sort(DueDateCalculator.ORDER);

        assertThat(sorted).containsExactly(overdueMuch, overdueLittle, soonKm, soonDays, okNear, okFar);
    }
}
