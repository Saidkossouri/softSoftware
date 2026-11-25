package fr.carsupervision.schedule;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestCatalogue.ASSURANCE;
import static fr.carsupervision.support.TestCatalogue.VIGNETTE;
import static fr.carsupervision.support.TestCatalogue.VISITE_TECHNIQUE;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * US4 : suggestions d'échéances administratives au Maroc (US4-9, FR-040, SC-013) : assurance et
 * vignette pour tout véhicule, visite technique à partir de 5 ans. Horloge en 2026.
 */
class DeadlineSuggestionsIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private Vehicle sixYears;
    private Vehicle fourYears;

    @BeforeEach
    void vehicles() {
        Member user = members.create(USER, Role.USER);
        sixYears = vehicles.active("Logan", 90_000, 2020);
        fourYears = vehicles.active("Clio", 50_000, 2022);
        vehicles.assign(user, sixYears);
        vehicles.assign(user, fourYears);
    }

    private ResultActions suggestions(String email, Vehicle vehicle) throws Exception {
        return mockMvc.perform(as(email, get("/api/vehicles/" + vehicle.getId() + "/deadline-suggestions")));
    }

    private ResultActions dismiss(String email, Vehicle vehicle, java.util.UUID typeId) throws Exception {
        return mockMvc.perform(as(email,
                post("/api/vehicles/" + vehicle.getId() + "/deadline-suggestions/" + typeId + "/dismiss")));
    }

    @Test
    void suggestsByVehicleAge() throws Exception {
        suggestions(USER, sixYears)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].type.name").value(contains("Assurance", "Vignette", "Visite technique")))
                .andExpect(jsonPath("$[0].recurrenceMonths").value(12))
                .andExpect(conformsToContract());
        suggestions(USER, fourYears)
                .andExpect(jsonPath("$[*].type.name").value(contains("Assurance", "Vignette")));

        Vehicle noYear = vehicles.active("Sans année", 10_000);
        suggestions(INITIAL_ADMIN, noYear)
                .andExpect(jsonPath("$[*].type.name").value(contains("Assurance", "Vignette")));
    }

    @Test
    void pendingDeadlineDismissalAndDeactivationRemoveSuggestions() throws Exception {
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + sixYears.getId() + "/deadlines"), """
                {"typeId":"%s","dueDate":"2026-09-01","recurrenceMonths":12}""".formatted(VISITE_TECHNIQUE))))
                .andExpect(status().isCreated());
        dismiss(USER, sixYears, VIGNETTE).andExpect(status().isNoContent());
        dismiss(USER, sixYears, VIGNETTE).andExpect(status().isNoContent());

        suggestions(USER, sixYears).andExpect(jsonPath("$[*].type.name").value(contains("Assurance")));
        suggestions(USER, fourYears).andExpect(jsonPath("$[*].type.name").value(contains("Assurance", "Vignette")));

        jdbc.update("UPDATE maintenance_type SET active = false WHERE id = ?", ASSURANCE);
        suggestions(USER, sixYears).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void accessAndArchive() throws Exception {
        members.create("autre@example.com", Role.USER);
        suggestions("autre@example.com", sixYears).andExpect(status().isForbidden()).andExpect(conformsToContract());
        dismiss("autre@example.com", sixYears, VIGNETTE).andExpect(status().isForbidden());
        dismiss(USER, sixYears, java.util.UUID.randomUUID()).andExpect(status().isNotFound());

        jdbc.update("UPDATE vehicle SET status = 'ARCHIVED' WHERE id = ?", sixYears.getId());
        suggestions(INITIAL_ADMIN, sixYears).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        dismiss(INITIAL_ADMIN, sixYears, VIGNETTE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ARCHIVED"));
    }
}
