package fr.carsupervision.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.carsupervision.common.error.Problems;
import fr.carsupervision.member.Member;
import fr.carsupervision.member.MemberRepository;
import fr.carsupervision.member.Role;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

/** Relecture du membre à chaque requête (FR-007, research R4). */
class CurrentMemberFilterTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final CurrentMemberFilter filter = new CurrentMemberFilter(memberRepository, new Problems(new ObjectMapper()));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void signedInWithGoogle(String email) {
        OidcIdToken token = OidcIdToken.withTokenValue("t").subject("s")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("email", email).build();
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), token), List.of(), "google"));
    }

    private static MockHttpServletRequest apiRequest(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
        request.setSession(session);
        return request;
    }

    @Test
    void revokedMemberGets403AndSessionIsInvalidated() throws Exception {
        signedInWithGoogle("enfant@example.com");
        when(memberRepository.findByEmail("enfant@example.com")).thenReturn(Optional.empty());
        MockHttpSession session = new MockHttpSession();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(apiRequest(session), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("MEMBER_REVOKED");
        assertThat(session.isInvalid()).isTrue();
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void currentRoleIsReadFromDatabase() throws Exception {
        signedInWithGoogle("Epouse@Example.com");
        Member member = new Member("epouse@example.com", null, Role.ADMIN, OffsetDateTime.now());
        when(memberRepository.findByEmail("epouse@example.com")).thenReturn(Optional.of(member));
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(apiRequest(new MockHttpSession()), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isInstanceOf(MemberAuthentication.class)
                .satisfies(auth -> assertThat(auth.getAuthorities())
                        .extracting(Object::toString).containsExactly("ROLE_ADMIN"));
    }
}
