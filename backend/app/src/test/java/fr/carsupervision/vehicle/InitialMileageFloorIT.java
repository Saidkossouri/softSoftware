package fr.carsupervision.vehicle;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestVehicles.reading;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.carsupervision.support.IntegrationTestBase;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** FR-009 : le kilométrage de départ ne dépasse pas le plus petit kilométrage saisi. */
class InitialMileageFloorIT extends IntegrationTestBase {

    private static String input(Vehicle vehicle, int initialKm) {
        return """
                {"nickname":"%s","make":"Dacia","model":"Logan","registration":"%s","initialMileageKm":%d}
                """.formatted(vehicle.getNickname(), vehicle.getRegistration(), initialKm);
    }

    @Test
    void initialMileageCannotExceedLowestRecordedMileage() throws Exception {
        Vehicle vehicle = vehicles.active("A", 47_000);
        mockMvc.perform(as(INITIAL_ADMIN, json(post("/api/vehicles/" + vehicle.getId() + "/mileage-readings"),
                        reading(LocalDate.of(2025, 6, 1), 48_000))))
                .andExpect(status().isCreated());

        mockMvc.perform(as(INITIAL_ADMIN, json(put("/api/vehicles/" + vehicle.getId()), input(vehicle, 49_000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INITIAL_MILEAGE_ABOVE_RECORDS"))
                .andExpect(jsonPath("$.detail").value(
                        "Le kilométrage de départ ne peut pas dépasser le plus petit kilométrage déjà saisi (48 000 km)."))
                .andExpect(conformsToContract());

        mockMvc.perform(as(INITIAL_ADMIN, json(put("/api/vehicles/" + vehicle.getId()), input(vehicle, 48_000))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialMileageKm").value(48000))
                // Le kilométrage actuel reste celui du relevé.
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(48000))
                .andExpect(jsonPath("$.currentMileage.date").value("2025-06-01"))
                .andExpect(conformsToContract());
    }
}
