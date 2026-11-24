package fr.carsupervision.common.error;

import fr.carsupervision.api.model.FieldError;
import fr.carsupervision.api.model.Problem;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Traduit toutes les erreurs de l'API en Problem Details avec messages en français (FR-025). */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String INVALID_INPUT = "Certains champs sont invalides. Corrigez-les puis réessayez.";

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Problem> handleApi(ApiException ex) {
        return Problems.response(ex.getStatus(), ex.getCode(), ex.getMessage(), ex.getFieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Problem> handleInvalidBody(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), ValidationMessages.toFrench(e)))
                .toList();
        return Problems.response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, INVALID_INPUT, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<Problem> handleInvalidParameters(HandlerMethodValidationException ex) {
        return Problems.response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, INVALID_INPUT, List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Problem> handleUnreadable(Exception ex) {
        return Problems.response(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "La requête est mal formée. Vérifiez les valeurs saisies.", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Problem> handleIntegrity(DataIntegrityViolationException ex) {
        String message = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
        if (message.contains("uk_member_email")) {
            return Problems.response(HttpStatus.CONFLICT, ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Cet email figure déjà dans la liste des membres.", List.of());
        }
        if (message.contains("uk_vehicle_registration_key")) {
            return Problems.response(HttpStatus.CONFLICT, ErrorCode.REGISTRATION_ALREADY_EXISTS,
                    "Cette immatriculation est déjà utilisée par un autre véhicule.", List.of());
        }
        LOG.error("Violation de contrainte inattendue", ex);
        return Problems.response(HttpStatus.CONFLICT, ErrorCode.VALIDATION_ERROR,
                "L'enregistrement est incompatible avec les données existantes.", List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Problem> handleAccessDenied(AccessDeniedException ex) {
        return Problems.response(HttpStatus.FORBIDDEN, ErrorCode.ADMIN_ONLY,
                "Cette action est réservée aux administrateurs.", List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Problem> handleNoResource(NoResourceFoundException ex) {
        return Problems.response(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND,
                "La ressource demandée n'existe pas.", List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Problem> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return Problems.response(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.VALIDATION_ERROR,
                "Cette opération n'est pas disponible.", List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Problem> handleUnexpected(Exception ex) {
        LOG.error("Erreur inattendue", ex);
        return Problems.response(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Une erreur inattendue est survenue. Réessayez plus tard.", List.of());
    }
}
