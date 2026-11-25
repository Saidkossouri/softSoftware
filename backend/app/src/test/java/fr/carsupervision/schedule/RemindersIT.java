package fr.carsupervision.schedule;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestVehicles.reading;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import fr.carsupervision.vehicle.Vehicle;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** US4 : rappels de l'accueil (US4-7, US4-8, FR-028, FR-035, SC-011). Horloge au 15/06/2026. */
class RemindersIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private Vehicle mine;
    private Vehicle other;
    private Vehicle archived;

    @BeforeEach
    void fleet() throws Exception {
        Member user = members.create(USER, Role.USER);
        mine = vehicles.active("Logan", 40_000);
        other = vehicles.active("Clio", 80_000);
        archived = vehicles.active("Ancienne", 150_000);
        vehicles.assign(user, mine);
        vehicles.assign(user, archived);

        deadline(mine, "Vignette", "2026-06-01");
        deadline(mine, "Assurance", "2026-07-01");
        deadline(mine, "Visite technique", "2027-03-01");
        deadline(other, "Assurance", "2026-06-10");
        deadline(archived, "Assurance", "2026-06-01");
        jdbc.update("UPDATE vehicle SET status = 'ARCHIVED' WHERE id = ?", archived.getId());
    }

    private void deadline(Vehicle vehicle, String label, String dueDate) throws Exception {
        mockMvc.perform(as(INITIAL_ADMIN, json(post("/api/vehicles/" + vehicle.getId() + "/deadlines"), """
                {"label":"%s","dueDate":"%s"}""".formatted(label, dueDate)))).andExpect(status().isCreated());
    }

    private ResultActions reminders(String email) throws Exception {
        return mockMvc.perform(as(email, get("/api/reminders")));
    }

    @Test
    void userSeesOverdueAndSoonOfAssignedActiveVehicles() throws Exception {
        reminders(USER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueItems[*].vehicleNickname").value(contains("Logan", "Logan")))
                .andExpect(jsonPath("$.dueItems[*].item.label").value(contains("Vignette", "Assurance")))
                .andExpect(jsonPath("$.dueItems[*].item.status").value(contains("OVERDUE", "SOON")))
                .andExpect(jsonPath("$.mileageReadings", hasSize(0)))
                .andExpect(conformsToContract());
    }

    @Test
    void adminSeesAllActiveVehicles() throws Exception {
        reminders(INITIAL_ADMIN)
                .andExpect(jsonPath("$.dueItems[*].vehicleNickname").value(contains("Logan", "Clio", "Logan")))
                .andExpect(jsonPath("$.dueItems[*].item.remainingDays").value(contains(-14, -5, 16)));
    }

    /** US4-8 : rappel de relevé à 31 jours sans kilométrage, pas à 30, disparu après un relevé. */
    @Test
    void remindsToRecordMileageAfterThirtyDays() throws Exception {
        clock.set(Instant.parse("2026-07-15T10:00:00Z"));
        reminders(USER).andExpect(jsonPath("$.mileageReadings", hasSize(0)));

        clock.set(Instant.parse("2026-07-16T10:00:00Z"));
        reminders(USER)
                .andExpect(jsonPath("$.mileageReadings", hasSize(1)))
                .andExpect(jsonPath("$.mileageReadings[0].vehicleNickname").value("Logan"))
                .andExpect(jsonPath("$.mileageReadings[0].currentMileage.mileageKm").value(40000))
                .andExpect(jsonPath("$.mileageReadings[0].currentMileage.date").value("2026-06-15"))
                .andExpect(conformsToContract());
        reminders(INITIAL_ADMIN)
                .andExpect(jsonPath("$.mileageReadings[*].vehicleNickname").value(contains("Clio", "Logan")));

        mockMvc.perform(as(USER, json(post("/api/vehicles/" + mine.getId() + "/mileage-readings"),
                reading(LocalDate.parse("2026-07-16"), 42_000)))).andExpect(status().isCreated());
        reminders(USER).andExpect(jsonPath("$.mileageReadings", hasSize(0)));
    }
}
