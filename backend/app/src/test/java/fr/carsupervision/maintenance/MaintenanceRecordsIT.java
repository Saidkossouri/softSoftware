package fr.carsupervision.maintenance;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestCatalogue.AUTRE;
import static fr.carsupervision.support.TestCatalogue.FILTRE_A_HUILE;
import static fr.carsupervision.support.TestCatalogue.VIDANGE;
import static fr.carsupervision.support.TestCatalogue.record;
import static fr.carsupervision.support.TestVehicles.reading;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import fr.carsupervision.vehicle.Vehicle;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** US2 : enregistrer un entretien (US2-1 à US2-6, FR-010 à FR-013). L'horloge est au 15/06/2026. */
class MaintenanceRecordsIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private Vehicle vehicle;

    @BeforeEach
    void vehicleAssignedToUser() {
        Member user = members.create(USER, Role.USER);
        vehicle = vehicles.active("A", 50_000);
        vehicles.assign(user, vehicle);
    }

    private ResultActions postRecord(String email, String body) throws Exception {
        return mockMvc.perform(as(email, json(post("/api/vehicles/" + vehicle.getId() + "/maintenance-records"),
                body)));
    }

    private String create(String email, String body) throws Exception {
        String response = postRecord(email, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void recordsCompleteMaintenanceAndUpdatesCurrentMileage() throws Exception {
        postRecord(USER, record("2026-06-15", 52_000,
                "\"costCents\":45000,\"garage\":\" Garage du Centre \",\"notes\":\"Huile 5W30\"",
                FILTRE_A_HUILE, VIDANGE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordDate").value("2026-06-15"))
                .andExpect(jsonPath("$.mileageKm").value(52000))
                .andExpect(jsonPath("$.types[0].name").value("Vidange"))
                .andExpect(jsonPath("$.types[1].name").value("Filtre à huile"))
                .andExpect(jsonPath("$.types[0].active").value(true))
                .andExpect(jsonPath("$.costCents").value(45000))
                .andExpect(jsonPath("$.garage").value("Garage du Centre"))
                .andExpect(jsonPath("$.notes").value("Huile 5W30"))
                .andExpect(jsonPath("$.authorName").value(USER))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.documentCount").value(0))
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(52000))
                .andExpect(conformsToContract());
    }

    @Test
    void validatesTypesDescriptionAndCost() throws Exception {
        postRecord(USER, record("2026-06-15", 51_000, null, AUTRE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("description"))
                .andExpect(conformsToContract());
        postRecord(USER, record("2026-06-15", 51_000, "\"description\":\"Essuie-glaces\"", AUTRE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Essuie-glaces"));

        postRecord(USER, record("2026-06-15", 51_000, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        // Doublon : refusé par le serveur (le contrat le dit en description).
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + vehicle.getId() + "/maintenance-records"),
                        record("2026-06-15", 51_000, null, VIDANGE, VIDANGE))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("typeIds"));
        postRecord(USER, record("2026-06-15", 51_000, "\"costCents\":-1", VIDANGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        postRecord(USER, record("2026-06-16", 51_000, null, VIDANGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("recordDate"));
    }

    @Test
    void sharesMileageConsistencyWithReadings() throws Exception {
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + vehicle.getId() + "/mileage-readings"),
                        reading(LocalDate.of(2026, 6, 10), 51_200))))
                .andExpect(status().isCreated());

        postRecord(USER, record("2026-06-15", 50_900, null, VIDANGE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MILEAGE_INCONSISTENT"))
                .andExpect(jsonPath("$.detail").value(containsString("51 200 km a été saisi le 10/06/2026")))
                .andExpect(conformsToContract());

        // Et un relevé est contrôlé par rapport aux entretiens.
        create(USER, record("2026-06-12", 52_000, null, VIDANGE));
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + vehicle.getId() + "/mileage-readings"),
                        reading(LocalDate.of(2026, 6, 15), 51_500))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MILEAGE_INCONSISTENT"));
    }

    @Test
    void backdatedMaintenanceIsAcceptedWithoutChangingCurrentMileage() throws Exception {
        create(USER, record("2026-06-15", 52_000, null, VIDANGE));

        postRecord(USER, record("2025-06-15", 48_000 + 2_500, "\"costCents\":120000", VIDANGE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(52000))
                .andExpect(jsonPath("$.currentMileage.date").value("2026-06-15"));
    }

    @Test
    void authorAndAdminCanEditAndDelete() throws Exception {
        String id = create(USER, record("2026-06-15", 52_000, null, VIDANGE));

        mockMvc.perform(as(USER, json(put("/api/maintenance-records/" + id),
                        record("2026-06-15", 52_100, "\"costCents\":46000", VIDANGE, FILTRE_A_HUILE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mileageKm").value(52100))
                .andExpect(jsonPath("$.types.length()").value(2))
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(52100))
                .andExpect(conformsToContract());

        mockMvc.perform(as(INITIAL_ADMIN, json(put("/api/maintenance-records/" + id),
                        record("2026-06-15", 52_200, null, VIDANGE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value(USER))
                .andExpect(jsonPath("$.costCents").doesNotExist());

        mockMvc.perform(as(USER, get("/api/maintenance-records/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mileageKm").value(52200))
                .andExpect(conformsToContract());

        mockMvc.perform(as(INITIAL_ADMIN, delete("/api/maintenance-records/" + id)))
                .andExpect(status().isNoContent());
        mockMvc.perform(as(USER, get("/api/vehicles/" + vehicle.getId())))
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(50000));
    }
}
