package fr.carsupervision.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

import fr.carsupervision.member.Member;
import fr.carsupervision.member.MemberRepository;
import fr.carsupervision.member.Role;
import java.time.OffsetDateTime;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

/** Fabriques de membres en base et de sessions Google simulées. */
@TestComponent
public class TestMembers {

    private final MemberRepository memberRepository;

    public TestMembers(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member create(String email, Role role) {
        return memberRepository.save(new Member(email, null, role, OffsetDateTime.now()));
    }

    /** Session Google d'un compte dont l'email est vérifié. */
    public static OidcLoginRequestPostProcessor google(String email) {
        return oidcLogin().idToken(token -> token
                .subject("google-" + email)
                .claim("email", email)
                .claim("email_verified", true)
                .claim("name", "Utilisateur " + email));
    }
}
