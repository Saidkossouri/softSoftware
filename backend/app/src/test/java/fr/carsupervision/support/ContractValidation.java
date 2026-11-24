package fr.carsupervision.support;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Vérifie qu'une requête et sa réponse respectent le contrat {@code openapi.yaml} (principe V).
 * À appliquer à chaque appel {@code /api} des tests d'intégration.
 */
public final class ContractValidation {

    private static final OpenApiInteractionValidator VALIDATOR = OpenApiInteractionValidator
            .createForSpecificationUrl("openapi/openapi.yaml")
            .withBasePathOverride("/api")
            .build();

    private ContractValidation() {
    }

    public static ResultMatcher conformsToContract() {
        return OpenApiValidationMatchers.openApi().isValid(VALIDATOR);
    }
}
