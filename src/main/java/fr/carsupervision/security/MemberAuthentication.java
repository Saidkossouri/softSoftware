package fr.carsupervision.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authentification d'une requête, construite par {@link CurrentMemberFilter} à partir du membre
 * lu en base. Elle n'est jamais enregistrée en session : seul l'email Google l'est.
 */
public class MemberAuthentication extends AbstractAuthenticationToken {

    private final AuthenticatedMember member;

    public MemberAuthentication(AuthenticatedMember member) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + member.role().name())));
        this.member = member;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public AuthenticatedMember getPrincipal() {
        return member;
    }

    @Override
    public String getName() {
        return member.email();
    }
}
