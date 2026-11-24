package fr.carsupervision.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Accès au membre connecté depuis les services. */
@Component
public class CurrentMember {

    public AuthenticatedMember get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MemberAuthentication memberAuthentication) {
            return memberAuthentication.getPrincipal();
        }
        throw new IllegalStateException("Aucun membre authentifié dans le contexte de la requête");
    }
}
