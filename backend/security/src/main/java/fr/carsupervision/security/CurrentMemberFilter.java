package fr.carsupervision.security;

import fr.carsupervision.member.Member;
import fr.carsupervision.member.MemberRepository;
import fr.carsupervision.common.Normalizers;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.common.error.Problems;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Relit le membre en base à chaque requête protégée (research R4) : un membre retiré est refusé
 * immédiatement (403 {@code MEMBER_REVOKED}) et son rôle actuel remplace celui de la session.
 */
public class CurrentMemberFilter extends OncePerRequestFilter {

    private final MemberRepository memberRepository;
    private final Problems problems;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public CurrentMemberFilter(MemberRepository memberRepository, Problems problems) {
        this.memberRepository = memberRepository;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ProtectedPaths.isProtected(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = contextHolder.getContext().getAuthentication();
        Optional<String> email = emailOf(authentication);
        if (email.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        Optional<Member> member = memberRepository.findByEmail(email.get());
        if (member.isEmpty()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            contextHolder.clearContext();
            problems.write(response, HttpStatus.FORBIDDEN, ErrorCode.MEMBER_REVOKED,
                    "Votre accès a été retiré. Demandez à un administrateur de vous ajouter de nouveau.");
            return;
        }
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(new MemberAuthentication(AuthenticatedMember.of(member.get())));
        contextHolder.setContext(context);
        chain.doFilter(request, response);
    }

    private static Optional<String> emailOf(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return Optional.ofNullable(Normalizers.normalizeEmail(oidcUser.getEmail()));
        }
        return Optional.empty();
    }
}
