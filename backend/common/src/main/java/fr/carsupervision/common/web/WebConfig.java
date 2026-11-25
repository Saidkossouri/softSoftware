package fr.carsupervision.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration web :
 * <ul>
 *   <li>les contrôleurs REST (qui implémentent les interfaces générées depuis le contrat) sont
 *       servis sous {@code /api}, l'URL du serveur déclarée dans {@code openapi.yaml} ;</li>
 *   <li>le contrat est exposé tel quel en {@code /openapi.yaml} pour Swagger UI.</li>
 * </ul>
 * La SPA n'est pas servie par le backend : Vite (développement) ou nginx (image du frontend)
 * la servent et relaient l'API vers le backend.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

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
    }
}
