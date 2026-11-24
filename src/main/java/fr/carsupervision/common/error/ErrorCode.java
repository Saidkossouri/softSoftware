package fr.carsupervision.common.error;

import fr.carsupervision.api.model.Problem;

/** Codes d'erreur stables du contrat ({@code Problem.code}). */
public enum ErrorCode {
    UNAUTHENTICATED,
    MEMBER_REVOKED,
    ADMIN_ONLY,
    VEHICLE_FORBIDDEN,
    NOT_FOUND,
    VALIDATION_ERROR,
    EMAIL_ALREADY_EXISTS,
    REGISTRATION_ALREADY_EXISTS,
    LAST_ADMIN,
    MEMBER_IS_ADMIN,
    CSRF_INVALID,
    INTERNAL_ERROR;

    public Problem.CodeEnum toContract() {
        return Problem.CodeEnum.fromValue(name());
    }
}
