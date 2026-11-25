package fr.carsupervision.maintenance;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestCatalogue.ASSURANCE;
import static fr.carsupervision.support.TestCatalogue.CLIMATISATION;
import static fr.carsupervision.support.TestCatalogue.VIDANGE;
import static fr.carsupervision.support.TestCatalogue.record;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** US2 : droits sur les entretiens (US2-5, FR-003, FR-004) et lecture du catalogue (FR-036). */
class MaintenanceAccessIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";
    private static final String OTHER = "fille@example.com";

    private Vehicle assigned;
    private Vehicle notAssigned;

    @BeforeEach
    void fleet() {
        Member user = members.create(USER, Role.USER);
        Member other = members.create(OTHER, Role.USER);
        assigned = vehicles.active("A", 50_000);
        notAssigned = vehicles.active("B", 20_000);
        vehicles.assign(user, assigned);
        vehicles.assign(other, assigned);
        vehicles.assign(other, notAssigned);
    }

    private String create(String email, Vehicle vehicle, int km) throws Exception {
        String body = mockMvc.perform(as(email, json(post("/api/vehicles/" + vehicle.getId() + "/maintenance-records"),
                        record("2026-06-15", km, null, VIDANGE))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void userCannotReadOrWriteMaintenanceOfVehicleNotAssigned() throws Exception {
        String recordOfB = create(OTHER, notAssigned, 21_000);

        mockMvc.perform(as(USER, get("/api/maintenance-records/" + recordOfB)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + notAssigned.getId() + "/maintenance-records"),
                        record("2026-06-15", 22_000, null, VIDANGE))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(USER, json(put("/api/maintenance-records/" + recordOfB),
                        record("2026-06-15", 22_000, null, VIDANGE))))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(USER, delete("/api/maintenance-records/" + recordOfB)))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(USER, get("/api/maintenance-records/" + UUID.randomUUID())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(INITIAL_ADMIN, get("/api/maintenance-records/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(conformsToContract());
    }

    @Test
    void userCanReadButNotEditMaintenanceOfAnotherMember() throws Exception {
        String id = create(OTHER, assigned, 51_000);

        mockMvc.perform(as(USER, get("/api/maintenance-records/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canEdit").value(false))
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, json(put("/api/maintenance-records/" + id),
                        record("2026-06-15", 51_100, null, VIDANGE))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_AUTHOR"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, delete("/api/maintenance-records/" + id)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_AUTHOR"));
    }

    @Test
    void archivedVehicleRefusesWritesButAdminStillReads() throws Exception {
        String id = create(USER, assigned, 51_000);
        jdbc.update("UPDATE vehicle SET status = 'ARCHIVED' WHERE id = ?", assigned.getId());

        mockMvc.perform(as(INITIAL_ADMIN, json(post("/api/vehicles/" + assigned.getId() + "/maintenance-records"),
                        record("2026-06-15", 52_000, null, VIDANGE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ARCHIVED"));
        mockMvc.perform(as(INITIAL_ADMIN, json(put("/api/maintenance-records/" + id),
                        record("2026-06-15", 51_500, null, VIDANGE))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ARCHIVED"));
        mockMvc.perform(as(INITIAL_ADMIN, get("/api/maintenance-records/" + id)))
                .andExpect(status().isOk())
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, get("/api/maintenance-records/" + id)))
                .andExpect(status().isForbidden());
    }

    @Test
    void everyMemberReadsThePrefilledActiveCatalogueInOrder() throws Exception {
        mockMvc.perform(as(USER, get("/api/maintenance-types")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0].name").value("Vidange"))
                .andExpect(jsonPath("$[0].defaultIntervalKm").value(10000))
                .andExpect(jsonPath("$[0].defaultIntervalMonths").value(12))
                .andExpect(jsonPath("$[12].name").value("Autre"))
                .andExpect(jsonPath("$[12].requiresDescription").value(true))
                .andExpect(jsonPath("$[15].name").value("Visite technique"))
                .andExpect(jsonPath("$[15].suggestFromAgeYears").value(5))
                .andExpect(conformsToContract());

        mockMvc.perform(as(USER, get("/api/maintenance-types").param("category", "ADMINISTRATIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Assurance", "Vignette", "Visite technique")));

        jdbc.update("UPDATE maintenance_type SET active = false WHERE id = ?", CLIMATISATION);
        mockMvc.perform(as(USER, get("/api/maintenance-types").param("category", "MAINTENANCE")))
                .andExpect(jsonPath("$", hasSize(12)));
        mockMvc.perform(as(USER, get("/api/maintenance-types").param("includeInactive", "true")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"))
                .andExpect(conformsToContract());
        mockMvc.perform(as(INITIAL_ADMIN, get("/api/maintenance-types").param("includeInactive", "true")))
                .andExpect(jsonPath("$", hasSize(16)));
    }

    @Test
    void refusesInactiveAdministrativeOrUnknownTypes() throws Exception {
        jdbc.update("UPDATE maintenance_type SET active = false WHERE id = ?", CLIMATISATION);

        for (UUID typeId : new UUID[] {CLIMATISATION, ASSURANCE, UUID.randomUUID()}) {
            mockMvc.perform(as(USER, json(post("/api/vehicles/" + assigned.getId() + "/maintenance-records"),
                            record("2026-06-15", 51_000, null, typeId))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("typeIds"))
                    .andExpect(conformsToContract());
        }
    }

    @Test
    void deactivatedTypeAlreadyOnRecordCanBeKept() throws Exception {
        String body = mockMvc.perform(as(USER, json(post("/api/vehicles/" + assigned.getId()
                                + "/maintenance-records"), record("2026-06-15", 51_000, null, CLIMATISATION))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        jdbc.update("UPDATE maintenance_type SET active = false WHERE id = ?", CLIMATISATION);

        mockMvc.perform(as(USER, json(put("/api/maintenance-records/" + id),
                        record("2026-06-15", 51_100, null, CLIMATISATION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types[0].name").value("Climatisation"))
                .andExpect(jsonPath("$.types[0].active").value(false));
    }
}
