package fr.carsupervision.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.carsupervision.api.model.FieldError;
import fr.carsupervision.api.model.Problem;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Construction et écriture des réponses Problem Details (RFC 9457) du contrat. */
@Component
public class Problems {

    private final ObjectMapper objectMapper;

    public Problems(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static Problem build(HttpStatus status, ErrorCode code, String detail, List<FieldError> fieldErrors) {
        Problem problem = new Problem(status.value(), detail, code.toContract());
        problem.setTitle(status.getReasonPhrase());
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            problem.setFieldErrors(List.copyOf(fieldErrors));
        }
        return problem;
    }

    public static ResponseEntity<Problem> response(HttpStatus status, ErrorCode code, String detail,
            List<FieldError> fieldErrors) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(build(status, code, detail, fieldErrors));
    }

    /** Écriture directe depuis un filtre de sécurité (hors Spring MVC). */
    public void write(HttpServletResponse response, HttpStatus status, ErrorCode code, String detail)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), build(status, code, detail, List.of()));
    }
}
