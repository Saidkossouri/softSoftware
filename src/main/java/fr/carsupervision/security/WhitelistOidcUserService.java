package fr.carsupervision.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Fin du flux Google : seuls les comptes dont l'email est vérifié et présent dans la liste
 * blanche obtiennent une session (FR-002). Les autres provoquent un échec de connexion, rendu en
 * 403 par {@link AccessDeniedLoginHandler}.
 */
public class WhitelistOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final MemberLoginService memberLoginService;

    public WhitelistOidcUserService(MemberLoginService memberLoginService) {
        this(new OidcUserService(), memberLoginService);
    }

    WhitelistOidcUserService(OAuth2UserService<OidcUserRequest, OidcUser> delegate,
            MemberLoginService memberLoginService) {
        this.delegate = delegate;
        this.memberLoginService = memberLoginService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser user = delegate.loadUser(userRequest);
        return memberLoginService.recordLogin(user.getEmail(), user.getEmailVerified(), user.getFullName())
                .map(member -> user)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED, "Compte non autorisé", null)));
    }
}
