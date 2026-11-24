package fr.carsupervision.member;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.TestMembers.google;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.carsupervision.support.IntegrationTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Histoire 2 : gestion des membres par un ADMIN (FR-010, FR-011, FR-013, FR-005). */
class MembersIT extends IntegrationTestBase {

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(google(INITIAL_ADMIN)).with(csrf());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void listsMembersSortedByDisplayNameThenEmail() throws Exception {
        members.create("zoe@example.com", Role.USER).setDisplayName(null);
        jdbc.update("UPDATE member SET display_name = 'Amina' WHERE email = ?", INITIAL_ADMIN);
        members.create("bob@example.com", Role.USER);
        jdbc.update("UPDATE member SET display_name = 'bruno' WHERE email = 'bob@example.com'");

        mockMvc.perform(asAdmin(get("/api/members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", contains(INITIAL_ADMIN, "bob@example.com", "zoe@example.com")))
                .andExpect(conformsToContract());
    }

    @Test
    void addsMemberWithNormalizedEmail() throws Exception {
        mockMvc.perform(asAdmin(json(post("/api/members"),
                        "{\"email\":\"Epouse@Example.com\",\"displayName\":\"  Leila \",\"role\":\"USER\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("epouse@example.com"))
                .andExpect(jsonPath("$.displayName").value("Leila"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.lastLoginAt").doesNotExist())
                .andExpect(conformsToContract());

        // Le membre ajouté peut se connecter immédiatement avec le rôle USER (US2-1).
        mockMvc.perform(get("/api/me").with(google("epouse@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void refusesDuplicateEmailWhateverTheCase() throws Exception {
        mockMvc.perform(asAdmin(json(post("/api/members"), "{\"email\":\"SAID@example.com\",\"role\":\"USER\"}")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(conformsToContract());
    }

    @Test
    void refusesMalformedEmailWithFieldError() throws Exception {
        mockMvc.perform(asAdmin(json(post("/api/members"), "{\"email\":\"pas-un-email\",\"role\":\"USER\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
        // Pas de validation de contrat ici : la requête est volontairement invalide.
    }

    @Test
    void refusesMissingRole() throws Exception {
        mockMvc.perform(asAdmin(json(post("/api/members"), "{\"email\":\"a@example.com\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("role"));
    }

    @Test
    void updatesDisplayNameAndRole() throws Exception {
        Member member = members.create("epouse@example.com", Role.USER);

        mockMvc.perform(asAdmin(json(patch("/api/members/" + member.getId()),
                        "{\"displayName\":\"Leila\",\"role\":\"ADMIN\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Leila"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(conformsToContract());

        // Chaîne vide : le nom affiché est effacé ; le rôle absent reste inchangé.
        mockMvc.perform(asAdmin(json(patch("/api/members/" + member.getId()), "{\"displayName\":\"\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").doesNotExist())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(conformsToContract());
    }

    @Test
    void refusesEmptyUpdate() throws Exception {
        Member member = members.create("epouse@example.com", Role.USER);

        mockMvc.perform(asAdmin(json(patch("/api/members/" + member.getId()), "{}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void removesMember() throws Exception {
        Member member = members.create("epouse@example.com", Role.USER);

        mockMvc.perform(asAdmin(delete("/api/members/" + member.getId())))
                .andExpect(status().isNoContent())
                .andExpect(conformsToContract());
        mockMvc.perform(asAdmin(get("/api/members")))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void unknownMemberReturns404() throws Exception {
        String unknown = "/api/members/" + UUID.randomUUID();
        mockMvc.perform(asAdmin(get(unknown)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(conformsToContract());
        mockMvc.perform(asAdmin(json(patch(unknown), "{\"role\":\"USER\"}")))
                .andExpect(status().isNotFound());
        mockMvc.perform(asAdmin(delete(unknown)))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyMemberOperationIsRefusedToUser() throws Exception {
        Member user = members.create("enfant@example.com", Role.USER);
        String self = "/api/members/" + user.getId();

        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[] {
            get("/api/members"),
            json(post("/api/members"), "{\"email\":\"x@example.com\",\"role\":\"ADMIN\"}"),
            get(self),
            json(patch(self), "{\"role\":\"ADMIN\"}"),
            delete(self),
        }) {
            mockMvc.perform(request.with(google("enfant@example.com")).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ADMIN_ONLY"))
                    .andExpect(conformsToContract());
        }
    }
}
