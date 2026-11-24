package fr.carsupervision.security;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.TestMembers.google;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

/** Les retraits et changements de rôle prennent effet dès la requête suivante (FR-007, SC-006). */
class ImmediateRevocationIT extends IntegrationTestBase {

    private void adminChangesRole(Member member, String role) throws Exception {
        mockMvc.perform(patch("/api/members/" + member.getId())
                        .with(google(INITIAL_ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void removedMemberIsRefusedOnNextRequest() throws Exception {
        Member user = members.create("enfant@example.com", Role.USER);
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/api/me").session(session).with(google("enfant@example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/members/" + user.getId()).with(google(INITIAL_ADMIN)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/me").session(session).with(google("enfant@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_REVOKED"))
                .andExpect(conformsToContract());
        // L'invalidation de la session est vérifiée par CurrentMemberFilterTest.
    }

    @Test
    void promotedUserGetsAdminRightsOnNextRequest() throws Exception {
        Member user = members.create("epouse@example.com", Role.USER);
        mockMvc.perform(get("/api/members").with(google("epouse@example.com")))
                .andExpect(status().isForbidden());

        adminChangesRole(user, "ADMIN");

        mockMvc.perform(get("/api/members").with(google("epouse@example.com")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me").with(google("epouse@example.com")))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void demotedAdminLosesAdminRightsOnNextRequest() throws Exception {
        Member admin = members.create("epouse@example.com", Role.ADMIN);
        mockMvc.perform(get("/api/members").with(google("epouse@example.com")))
                .andExpect(status().isOk());

        adminChangesRole(admin, "USER");

        mockMvc.perform(get("/api/members").with(google("epouse@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"));
    }
}
