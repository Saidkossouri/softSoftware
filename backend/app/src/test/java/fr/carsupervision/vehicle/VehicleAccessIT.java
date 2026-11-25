package fr.carsupervision.vehicle;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.TestMembers.google;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Histoire 4 : attribution des véhicules aux membres USER par un ADMIN (FR-019, FR-020). */
class VehicleAccessIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(google(INITIAL_ADMIN)).with(csrf());
    }

    private static MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return request.with(google(USER)).with(csrf());
    }

    private static MockHttpServletRequestBuilder access(MockHttpServletRequestBuilder request, String... ids) {
        String body = ids.length == 0 ? "" : "\"" + String.join("\",\"", ids) + "\"";
        return request.contentType(MediaType.APPLICATION_JSON).content("{\"vehicleIds\":[" + body + "]}");
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

    @Test
    void replacesTheWholeSetOfVehicles() throws Exception {
        Member user = members.create(USER, Role.USER);
        String a = createVehicle("A", "AA-111-AA");
        String b = createVehicle("B", "BB-222-BB");
        String c = createVehicle("C", "CC-333-CC");

        mockMvc.perform(asAdmin(get("/api/members/" + user.getId() + "/vehicles")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleIds", hasSize(0)))
                .andExpect(conformsToContract());

        mockMvc.perform(asAdmin(access(put("/api/members/" + user.getId() + "/vehicles"), a, b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleIds", containsInAnyOrder(a, b)))
                .andExpect(conformsToContract());

        mockMvc.perform(asAdmin(access(put("/api/members/" + user.getId() + "/vehicles"), b, c)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleIds", containsInAnyOrder(b, c)));

        mockMvc.perform(asAdmin(get("/api/members/" + user.getId() + "/vehicles")))
                .andExpect(jsonPath("$.vehicleIds", containsInAnyOrder(b, c)))
                .andExpect(conformsToContract());

        // Liste vide : plus aucun véhicule.
        mockMvc.perform(asAdmin(access(put("/api/members/" + user.getId() + "/vehicles"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleIds", hasSize(0)))
                .andExpect(conformsToContract());
    }

    @Test
    void keepsGrantDateOfUnchangedAccess() throws Exception {
        Member user = members.create(USER, Role.USER);
        String a = createVehicle("A", "AA-111-AA");
        String b = createVehicle("B", "BB-222-BB");
        mockMvc.perform(asAdmin(access(put("/api/members/" + user.getId() + "/vehicles"), a)))
                .andExpect(status().isOk());
        OffsetDateTime first = grantedAt(user.getId(), a);

        mockMvc.perform(asAdmin(access(put("/api/members/" + user.getId() + "/vehicles"), a, b)))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(grantedAt(user.getId(), a)).isEqualTo(first);
    }

    private OffsetDateTime grantedAt(UUID memberId, String vehicleId) {
        return jdbc.queryForObject("select granted_at from vehicle_access where member_id = ? and vehicle_id = ?",
                OffsetDateTime.class, memberId, UUID.fromString(vehicleId));
    }

    @Test
    void archivedVehiclesCanBeAssignedAndAreListed() throws Exception {
        Member user = members.create(USER, Role.USER);
        String a = createVehicle("A", "AA-111-AA");
        mockMvc.perform(asAdmin(post("/api/vehicles/" + a + "/archive"))).andExpect(status().isOk());

        mockMvc.perform(asAdmin(access(put("/api/members/" + user.getId() + "/vehicles"), a)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleIds", containsInAnyOrder(a)));
    }

    @Test
    void refusesUnknownVehicle() throws Exception {
        Member user = members.create(USER, Role.USER);
        String a = createVehicle("A", "AA-111-AA");

        mockMvc.perform(asAdmin(access(put("/api/members/" + user.getId() + "/vehicles"),
                        a, UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("vehicleIds"))
                .andExpect(conformsToContract());

        // Rien n'a été enregistré.
        mockMvc.perform(asAdmin(get("/api/members/" + user.getId() + "/vehicles")))
                .andExpect(jsonPath("$.vehicleIds", hasSize(0)));
    }

    @Test
    void refusesAdminMember() throws Exception {
        Member admin = members.create("papa@example.com", Role.ADMIN);
        String a = createVehicle("A", "AA-111-AA");

        mockMvc.perform(asAdmin(access(put("/api/members/" + admin.getId() + "/vehicles"), a)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER_IS_ADMIN"))
                .andExpect(conformsToContract());
    }

    @Test
    void unknownMemberIsNotFound() throws Exception {
        String unknown = UUID.randomUUID().toString();

        mockMvc.perform(asAdmin(get("/api/members/" + unknown + "/vehicles")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(conformsToContract());
        mockMvc.perform(asAdmin(access(put("/api/members/" + unknown + "/vehicles"))))
                .andExpect(status().isNotFound())
                .andExpect(conformsToContract());
    }

    @Test
    void userCannotReadOrChangeAssignments() throws Exception {
        Member user = members.create(USER, Role.USER);
        String a = createVehicle("A", "AA-111-AA");

        mockMvc.perform(asUser(get("/api/members/" + user.getId() + "/vehicles")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"))
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(access(put("/api/members/" + user.getId() + "/vehicles"), a)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"))
                .andExpect(conformsToContract());

        mockMvc.perform(asAdmin(get("/api/members/" + user.getId() + "/vehicles")))
                .andExpect(jsonPath("$.vehicleIds", hasSize(0)));
    }

    @Test
    void removingMemberRemovesAssignments() throws Exception {
        Member user = members.create(USER, Role.USER);
        String a = createVehicle("A", "AA-111-AA");
        mockMvc.perform(asAdmin(access(put("/api/members/" + user.getId() + "/vehicles"), a)))
                .andExpect(status().isOk());

        mockMvc.perform(asAdmin(delete("/api/members/" + user.getId()))).andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("select count(*) from vehicle_access", Long.class)).isZero();
    }
}
