package fr.carsupervision.common;

import static fr.carsupervision.support.TestMembers.google;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.carsupervision.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Pages hors API servies par le backend : contrat et Swagger UI réservés aux membres connectés
 * (principe I), santé et page « Accès refusé » publiques. La SPA est servie par Vite ou nginx,
 * jamais par le backend.
 */
class StaticAndDocsIT extends IntegrationTestBase {

    @Test
    void contractRequiresSession() throws Exception {
        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/"));

        mockMvc.perform(get("/openapi.yaml").with(google(INITIAL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3")));
    }

    @Test
    void swaggerUiRequiresSession() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/"));

        mockMvc.perform(get("/swagger-ui/index.html").with(google(INITIAL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
    }

    @Test
    void swaggerUiShowsTheContractItself() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config").with(google(INITIAL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/openapi.yaml"));
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void accessDeniedPageIsPublic() throws Exception {
        mockMvc.perform(get("/access-denied.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Accès refusé")));
    }

    @Test
    void backendDoesNotServeSpaRoutes() throws Exception {
        mockMvc.perform(get("/admin/parc"))
                .andExpect(status().isNotFound());
    }
}
