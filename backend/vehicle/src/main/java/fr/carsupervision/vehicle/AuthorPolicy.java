package fr.carsupervision.vehicle;

import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.security.AuthenticatedMember;
import fr.carsupervision.security.CurrentMember;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Règle d'auteur des saisies (relevés, entretiens, règles, échéances) : seul l'auteur ou un ADMIN
 * modifie ou supprime (research R6). L'auteur est enregistré avec un instantané de son nom, qui
 * reste affiché si le membre est ensuite retiré de la liste blanche.
 */
@Component
public class AuthorPolicy {

    static final String NOT_AUTHOR_MESSAGE = "Seul l'auteur de cette saisie ou un administrateur peut la modifier.";
    private static final int AUTHOR_NAME_MAX_LENGTH = 100;

    private final CurrentMember currentMember;

    public AuthorPolicy(CurrentMember currentMember) {
        this.currentMember = currentMember;
    }

    /** Auteur d'une nouvelle saisie : identifiant et nom affiché (ou email à défaut). */
    public record AuthorSnapshot(UUID id, String name) {
    }

    public AuthorSnapshot authorSnapshot() {
        AuthenticatedMember member = currentMember.get();
        String name = member.displayName() == null || member.displayName().isBlank()
                ? member.email()
                : member.displayName().strip();
        if (name.length() > AUTHOR_NAME_MAX_LENGTH) {
            name = name.substring(0, AUTHOR_NAME_MAX_LENGTH);
        }
        return new AuthorSnapshot(member.id(), name);
    }

    /** Vrai si le membre connecté est l'auteur ou un ADMIN ({@code canEdit} du contrat). */
    public boolean canEdit(UUID authorId) {
        AuthenticatedMember member = currentMember.get();
        return member.isAdmin() || member.id().equals(authorId);
    }

    /** Lève 403 {@code NOT_AUTHOR} si le membre connecté n'est ni l'auteur ni un ADMIN. */
    public void assertAuthorOrAdmin(UUID authorId) {
        if (!canEdit(authorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.NOT_AUTHOR, NOT_AUTHOR_MESSAGE);
        }
    }
}
