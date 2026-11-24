package fr.carsupervision.common.error;

import fr.carsupervision.api.model.FieldError;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Erreur métier traduite en Problem Details par {@link ApiExceptionHandler}. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode code;
    private final List<FieldError> fieldErrors;

    public ApiException(HttpStatus status, ErrorCode code, String detail) {
        this(status, code, detail, List.of());
    }

    public ApiException(HttpStatus status, ErrorCode code, String detail, List<FieldError> fieldErrors) {
        super(detail);
        this.status = status;
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, detail);
    }

    public static ApiException conflict(ErrorCode code, String detail) {
        return new ApiException(HttpStatus.CONFLICT, code, detail);
    }

    public static ApiException invalidField(String field, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Certains champs sont invalides. Corrigez-les puis réessayez.",
                List.of(new FieldError(field, message)));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
