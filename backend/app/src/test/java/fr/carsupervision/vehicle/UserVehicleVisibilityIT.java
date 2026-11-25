package fr.carsupervision.vehicle;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.TestMembers.google;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Histoire 4 : règle de lecture d'un véhicule (data-model, FR-021, FR-022). Un USER ne voit et
 * n'obtient que ses véhicules actifs attribués ; tous les autres cas donnent le même 403.
 */
class UserVehicleVisibilityIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private Member user;
    private String a;
    private String b;

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(google(INITIAL_ADMIN)).with(csrf());
    }

    private static MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return request.with(google(USER)).with(csrf());
    }

    private String createVehicle(String nickname, String registration) throws Exception {
        String body = mockMvc.perform(asAdmin(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"%s","make":"Renault","model":"Clio","registration":"%s","initialMileageKm":0}
                                """.formatted(nickname, registration))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private void assign(String... ids) throws Exception {
        String body = ids.length == 0 ? "" : "\"" + String.join("\",\"", ids) + "\"";
        mockMvc.perform(asAdmin(put("/api/members/" + user.getId() + "/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleIds\":[" + body + "]}")))
                .andExpect(status().isOk());
    }

    private void expectForbidden(String vehicleId) throws Exception {
        mockMvc.perform(asUser(get("/api/vehicles/" + vehicleId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"))
                .andExpect(jsonPath("$.detail").value("Vous n'avez pas accès à ce véhicule."))
                .andExpect(conformsToContract());
    }

    @BeforeEach
    void fleet() throws Exception {
        user = members.create(USER, Role.USER);
        a = createVehicle("A", "AA-111-AA");
        b = createVehicle("B", "BB-222-BB");
    }

    @Test
    void userSeesOnlyAssignedVehicles() throws Exception {
        assign(a);

        mockMvc.perform(asUser(get("/api/vehicles")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(a)))
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(get("/api/vehicles/" + a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("A"))
                .andExpect(conformsToContract());
    }

    @Test
    void sameRefusalForUnassignedArchivedAndUnknownVehicles() throws Exception {
        String archived = createVehicle("Archivée", "CC-333-CC");
        assign(a, archived);
        mockMvc.perform(asAdmin(post("/api/vehicles/" + archived + "/archive"))).andExpect(status().isOk());

        expectForbidden(b);
        expectForbidden(archived);
        expectForbidden(UUID.randomUUID().toString());

        mockMvc.perform(asUser(get("/api/vehicles")))
                .andExpect(jsonPath("$[*].id", contains(a)));
    }

    @Test
    void userCannotListArchivedVehicles() throws Exception {
        assign(a);

        mockMvc.perform(asUser(get("/api/vehicles").param("status", "ARCHIVED")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"))
                .andExpect(conformsToContract());
    }

    @Test
    void userWithoutAssignmentSeesEmptyList() throws Exception {
        mockMvc.perform(asUser(get("/api/vehicles")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(conformsToContract());
    }

    @Test
    void removedAssignmentTakesEffectOnNextRequest() throws Exception {
        assign(a);
        mockMvc.perform(asUser(get("/api/vehicles/" + a))).andExpect(status().isOk());

        assign();

        expectForbidden(a);
        mockMvc.perform(asUser(get("/api/vehicles"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void archivingThenReactivatingRestoresVisibility() throws Exception {
        assign(a);

        mockMvc.perform(asAdmin(post("/api/vehicles/" + a + "/archive"))).andExpect(status().isOk());
        expectForbidden(a);
        mockMvc.perform(asUser(get("/api/vehicles"))).andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(asAdmin(post("/api/vehicles/" + a + "/reactivate"))).andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/vehicles/" + a))).andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/vehicles"))).andExpect(jsonPath("$[*].id", contains(a)));
    }

    @Test
    void promotedMemberSeesWholeFleetAndKeepsAssignmentsForLater() throws Exception {
        assign(a);

        mockMvc.perform(asAdmin(patch("/api/members/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}")))
                .andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/vehicles"))).andExpect(jsonPath("$[*].id", contains(a, b)));

        mockMvc.perform(asAdmin(patch("/api/members/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}")))
                .andExpect(status().isOk());
        mockMvc.perform(asUser(get("/api/vehicles"))).andExpect(jsonPath("$[*].id", contains(a)));
    }

    @Test
    void adminSeesEverythingWithoutAssignment() throws Exception {
        mockMvc.perform(asAdmin(get("/api/vehicles")))
                .andExpect(jsonPath("$[*].id", contains(a, b)))
                .andExpect(conformsToContract());
        mockMvc.perform(asAdmin(get("/api/vehicles/" + b))).andExpect(status().isOk());
        mockMvc.perform(asAdmin(get("/api/vehicles/" + UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(conformsToContract());
    }
}
