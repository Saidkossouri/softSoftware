package fr.carsupervision.support;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
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

    /**
     * Envoi de fichier ({@code multipart}) fait avec MockMvc : le corps n'est pas transmis au
     * validateur, et le jeton CSRF de test y apparaît comme paramètre de requête. Ces deux messages
     * sont ignorés pour les envois de fichiers (le contrat n'y déclare aucun paramètre de requête) ;
     * la réponse reste validée.
     */
    private static final OpenApiInteractionValidator MULTIPART_VALIDATOR = OpenApiInteractionValidator
            .createForSpecificationUrl("openapi/openapi.yaml")
            .withBasePathOverride("/api")
            .withLevelResolver(LevelResolver.create()
                    .withLevel("validation.request.body.missing", ValidationReport.Level.IGNORE)
                    .withLevel("validation.request.parameter.query.unexpected", ValidationReport.Level.IGNORE)
                    .build())
            .build();

    private ContractValidation() {
    }

    /** Pour un envoi de fichier ({@code multipart}) fait avec MockMvc. */
    public static ResultMatcher conformsToContractForUpload() {
        return OpenApiValidationMatchers.openApi().isValid(MULTIPART_VALIDATOR);
    }

    public static ResultMatcher conformsToContract() {
        return OpenApiValidationMatchers.openApi().isValid(VALIDATOR);
    }
}
