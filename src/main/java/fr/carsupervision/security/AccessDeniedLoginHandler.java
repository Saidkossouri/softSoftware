package fr.carsupervision.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/** Connexion Google refusée : réponse 403 avec une page explicative, sans aucune donnée (FR-002). */
public class AccessDeniedLoginHandler implements AuthenticationFailureHandler {

    private static final String PAGE = "static/access-denied.html";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");
        try (InputStream page = new ClassPathResource(PAGE).getInputStream()) {
            page.transferTo(response.getOutputStream());
        }
    }
}
