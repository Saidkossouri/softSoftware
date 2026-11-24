package fr.carsupervision.common.web;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Configuration web :
 * <ul>
 *   <li>les contrôleurs REST (qui implémentent les interfaces générées depuis le contrat) sont
 *       servis sous {@code /api}, l'URL du serveur déclarée dans {@code openapi.yaml} ;</li>
 *   <li>le contrat est exposé tel quel en {@code /openapi.yaml} pour Swagger UI ;</li>
 *   <li>les routes de la SPA retombent sur {@code index.html} (research R7), sans contrôleur.</li>
 * </ul>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] NON_SPA_PREFIXES = {
        "api/", "oauth2/", "login/", "logout", "swagger-ui", "v3/", "actuator/"
    };

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api",
                HandlerTypePredicate.forAnnotation(RestController.class)
                        .and(HandlerTypePredicate.forBasePackage("fr.carsupervision")));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/openapi.yaml")
                .addResourceLocations("classpath:/openapi/");

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (isSpaRoute(resourcePath)) {
                            Resource index = new ClassPathResource("/static/index.html");
                            return index.exists() ? index : null;
                        }
                        return null;
                    }
                });
    }

    private static boolean isSpaRoute(String path) {
        for (String prefix : NON_SPA_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        // Un fichier (avec extension) absent reste une 404 ; une route de la SPA n'en a pas.
        return !path.substring(path.lastIndexOf('/') + 1).contains(".");
    }
}
