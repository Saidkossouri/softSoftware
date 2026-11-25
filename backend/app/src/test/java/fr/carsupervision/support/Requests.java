package fr.carsupervision.support;

import static fr.carsupervision.support.TestMembers.google;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Raccourcis des tests d'intégration : session Google, CSRF et corps JSON. */
public final class Requests {

    private Requests() {
    }

    public static MockHttpServletRequestBuilder as(String email, MockHttpServletRequestBuilder request) {
        return request.with(google(email)).with(csrf());
    }

    public static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
