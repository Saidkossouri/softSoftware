package fr.carsupervision.security;

import fr.carsupervision.administration.member.Member;
import fr.carsupervision.administration.member.Role;
import java.util.UUID;

/** Instantané du membre connecté, relu en base à chaque requête (FR-007). */
public record AuthenticatedMember(UUID id, String email, String displayName, Role role) {

    public static AuthenticatedMember of(Member member) {
        return new AuthenticatedMember(member.getId(), member.getEmail(), member.getDisplayName(), member.getRole());
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
