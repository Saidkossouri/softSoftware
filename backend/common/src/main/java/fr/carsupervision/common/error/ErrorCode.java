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
    INTERNAL_ERROR,
    VEHICLE_ARCHIVED,
    NOT_AUTHOR,
    MILEAGE_INCONSISTENT,
    MILEAGE_JUMP_NOT_CONFIRMED,
    INITIAL_MILEAGE_ABOVE_RECORDS,
    RULE_ALREADY_EXISTS,
    DOCUMENT_TYPE_NOT_ALLOWED,
    DOCUMENT_TOO_LARGE,
    DOCUMENT_LIMIT_REACHED,
    TYPE_NAME_ALREADY_EXISTS;

    public Problem.CodeEnum toContract() {
        return Problem.CodeEnum.fromValue(name());
    }
}
