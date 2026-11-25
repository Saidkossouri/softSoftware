package fr.carsupervision.maintenance;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestVehicles.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US1 : droits sur les relevés (FR-003, FR-004, US1-5, research R5 et R6). */
class MileageAccessIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";
    private static final String OTHER = "fille@example.com";
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private Member user;
    private Vehicle assigned;
    private Vehicle notAssigned;

    @BeforeEach
    void fleet() {
        user = members.create(USER, Role.USER);
        Member other = members.create(OTHER, Role.USER);
        assigned = vehicles.active("A", 50_000);
        notAssigned = vehicles.active("B", 20_000);
        vehicles.assign(user, assigned);
        vehicles.assign(other, assigned);
        vehicles.assign(other, notAssigned);
    }

    private String create(String email, Vehicle vehicle, int km) throws Exception {
        String body = mockMvc.perform(as(email, json(post("/api/vehicles/" + vehicle.getId() + "/mileage-readings"),
                        reading(TODAY, km))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void userCannotWriteOnVehicleNotAssigned() throws Exception {
        String readingOfB = create(OTHER, notAssigned, 21_000);

        mockMvc.perform(as(USER, json(post("/api/vehicles/" + notAssigned.getId() + "/mileage-readings"),
                        reading(TODAY, 22_000))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, json(put("/api/mileage-readings/" + readingOfB), reading(TODAY, 22_000))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"));
        mockMvc.perform(as(USER, delete("/api/mileage-readings/" + readingOfB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"));
    }

    @Test
    void unknownReadingIsForbiddenForUserAndNotFoundForAdmin() throws Exception {
        String unknown = "/api/mileage-readings/" + UUID.randomUUID();

        mockMvc.perform(as(USER, delete(unknown)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"));
        mockMvc.perform(as(USER, json(put(unknown), reading(TODAY, 51_000))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + UUID.randomUUID() + "/mileage-readings"),
                        reading(TODAY, 51_000))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(INITIAL_ADMIN, delete(unknown)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(INITIAL_ADMIN, json(post("/api/vehicles/" + UUID.randomUUID() + "/mileage-readings"),
                        reading(TODAY, 51_000))))
                .andExpect(status().isNotFound());
    }

    @Test
    void archivedVehicleRefusesAnyEntryEvenForAdmin() throws Exception {
        String readingId = create(USER, assigned, 51_000);
        jdbc.update("UPDATE vehicle SET status = 'ARCHIVED' WHERE id = ?", assigned.getId());

        mockMvc.perform(as(INITIAL_ADMIN, json(post("/api/vehicles/" + assigned.getId() + "/mileage-readings"),
                        reading(TODAY, 52_000))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ARCHIVED"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(INITIAL_ADMIN, delete("/api/mileage-readings/" + readingId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ARCHIVED"))
                .andExpect(conformsToContract());
        // Un USER ne voit plus le véhicule archivé : même refus que pour un véhicule non attribué.
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + assigned.getId() + "/mileage-readings"),
                        reading(TODAY, 52_000))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"));
    }

    @Test
    void onlyAuthorOrAdminCanEditOrDelete() throws Exception {
        String readingId = create(OTHER, assigned, 51_000);

        mockMvc.perform(as(USER, json(put("/api/mileage-readings/" + readingId), reading(TODAY, 51_100))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_AUTHOR"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, delete("/api/mileage-readings/" + readingId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_AUTHOR"));

        mockMvc.perform(as(INITIAL_ADMIN, json(put("/api/mileage-readings/" + readingId), reading(TODAY, 51_100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mileageKm").value(51100))
                .andExpect(jsonPath("$.authorName").value(OTHER))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(conformsToContract());
    }

    @Test
    void readingIsKeptWithAuthorNameWhenAuthorIsRemoved() throws Exception {
        String readingId = create(USER, assigned, 51_000);

        mockMvc.perform(as(INITIAL_ADMIN, delete("/api/members/" + user.getId())))
                .andExpect(status().isNoContent());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT author_id, author_name FROM mileage_reading WHERE id = ?", UUID.fromString(readingId));
        assertThat(row.get("author_id")).isNull();
        assertThat(row.get("author_name")).isEqualTo(USER);
        mockMvc.perform(as(INITIAL_ADMIN, json(put("/api/mileage-readings/" + readingId), reading(TODAY, 51_100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value(USER));
    }
}
