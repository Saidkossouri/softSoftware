package fr.carsupervision.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.carsupervision.member.Member;
import fr.carsupervision.member.MemberRepository;
import fr.carsupervision.member.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Liste blanche à la connexion (FR-002, FR-005, research R3). */
class WhitelistOidcUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T08:00:00Z");

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    @SuppressWarnings("unchecked")
    private final OAuth2UserService<OidcUserRequest, OidcUser> google = mock(OAuth2UserService.class);
    private WhitelistOidcUserService service;

    @BeforeEach
    void setUp() {
        MemberLoginService loginService = new MemberLoginService(memberRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        service = new WhitelistOidcUserService(google, loginService);
    }

    private void googleReturns(String email, Boolean verified, String name) {
        OidcIdToken.Builder token = OidcIdToken.withTokenValue("token")
                .subject("sub").issuedAt(NOW).expiresAt(NOW.plusSeconds(60))
                .claim("email", email);
        if (verified != null) {
            token.claim("email_verified", verified);
        }
        if (name != null) {
            token.claim("name", name);
        }
        when(google.loadUser(any())).thenReturn(new DefaultOidcUser(List.of(), token.build()));
    }

    private static Member member(String email, String displayName) {
        return new Member(email, displayName, Role.USER, OffsetDateTime.now());
    }

    @Test
    void refusesUnknownEmail() {
        googleReturns("inconnu@example.com", true, "Inconnu");
        when(memberRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(e -> ((OAuth2AuthenticationException) e).getError().getErrorCode())
                .isEqualTo("access_denied");
    }

    @Test
    void refusesUnverifiedEmailEvenIfWhitelisted() {
        googleReturns("said@example.com", false, "Said");
        when(memberRepository.findByEmail("said@example.com")).thenReturn(Optional.of(member("said@example.com", null)));

        assertThatThrownBy(() -> service.loadUser(null)).isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void refusesWhenVerificationClaimIsMissing() {
        googleReturns("said@example.com", null, "Said");
        when(memberRepository.findByEmail("said@example.com")).thenReturn(Optional.of(member("said@example.com", null)));

        assertThatThrownBy(() -> service.loadUser(null)).isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void acceptsWhitelistedEmailIgnoringCaseAndSpaces() {
        Member said = member("said@example.com", null);
        googleReturns(" Said@Example.COM ", true, "Said K.");
        when(memberRepository.findByEmail("said@example.com")).thenReturn(Optional.of(said));

        OidcUser user = service.loadUser(null);

        assertThat(user.getEmail()).isEqualTo(" Said@Example.COM ");
        assertThat(said.getLastLoginAt()).isEqualTo(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(said.getDisplayName()).isEqualTo("Said K.");
    }

    @Test
    void keepsDisplayNameChosenByAdmin() {
        Member said = member("said@example.com", "Papa");
        googleReturns("said@example.com", true, "Said K.");
        when(memberRepository.findByEmail("said@example.com")).thenReturn(Optional.of(said));

        service.loadUser(null);

        assertThat(said.getDisplayName()).isEqualTo("Papa");
    }

    @Test
    void ignoresGoogleNameWhenBlank() {
        Member said = member("said@example.com", null);
        googleReturns("said@example.com", true, "   ");
        when(memberRepository.findByEmail("said@example.com")).thenReturn(Optional.of(said));

        service.loadUser(null);

        assertThat(said.getDisplayName()).isNull();
    }
}
