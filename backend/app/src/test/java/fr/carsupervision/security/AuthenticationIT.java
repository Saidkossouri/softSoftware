package fr.carsupervision.security;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.TestMembers.google;
import static org.assertj.core.api.Assertions.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/** Histoire 1 : connexion, refus et déconnexion (US1, FR-002, FR-006, FR-008, FR-014). */
class AuthenticationIT extends IntegrationTestBase {

    @Test
    void anonymousApiCallIsRejectedWith401AndNoData() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(conformsToContract());
    }

    @Test
    void initialAdminSeesOwnIdentityAndAdminRole() throws Exception {
        mockMvc.perform(get("/api/me").with(google("Said@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(INITIAL_ADMIN))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(conformsToContract());
    }

    @Test
    void whitelistedUserSeesUserRole() throws Exception {
        members.create("epouse@example.com", Role.USER);

        mockMvc.perform(get("/api/me").with(google("epouse@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(conformsToContract());
    }

    @Test
    void sessionOfAccountAbsentFromWhitelistIsRejected() throws Exception {
        mockMvc.perform(get("/api/me").with(google("inconnu@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_REVOKED"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(conformsToContract());
    }

    @Test
    void refusedGoogleLoginRendersForbiddenPage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new AccessDeniedLoginHandler().onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied")));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("text/html");
        assertThat(response.getContentAsString()).contains("Accès refusé", "administrateur");
    }

    @Test
    void accessDeniedPageIsPublic() throws Exception {
        mockMvc.perform(get("/access-denied.html"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString(UTF_8)).contains("Accès refusé"));
    }

    @Test
    void logoutClosesSession() throws Exception {
        mockMvc.perform(post("/logout").with(google(INITIAL_ADMIN)).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutWithoutCsrfTokenIsRefused() throws Exception {
        mockMvc.perform(post("/logout").with(google(INITIAL_ADMIN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void unknownApiPathReturnsProblem() throws Exception {
        mockMvc.perform(get("/api/inexistant").with(google(INITIAL_ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void apiDocumentationRequiresSession() throws Exception {
        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/openapi.yaml").with(google(INITIAL_ADMIN)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui/index.html").with(google(INITIAL_ADMIN)))
                .andExpect(status().isOk());
    }
}
