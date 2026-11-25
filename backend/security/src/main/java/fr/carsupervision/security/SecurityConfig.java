package fr.carsupervision.security;

import fr.carsupervision.member.MemberRepository;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.common.error.Problems;
import java.util.LinkedHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Sécurité de l'application : connexion Google, liste blanche, CSRF et réponses JSON de l'API. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    static final String[] PUBLIC_PATHS = {"/", "/access-denied.html", "/actuator/health", "/error"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, MemberRepository memberRepository,
            MemberLoginService memberLoginService, Problems problems) throws Exception {
        RequestMatcher apiRequests = request -> ProtectedPaths.isApi(
                request.getRequestURI().substring(request.getContextPath().length()));

        AuthenticationEntryPoint apiEntryPoint = (request, response, ex) -> problems.write(response,
                HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHENTICATED,
                "Vous devez vous connecter avec votre compte Google.");

        AccessDeniedHandler accessDenied = (request, response, ex) -> {
            if (ex instanceof CsrfException) {
                problems.write(response, HttpStatus.FORBIDDEN, ErrorCode.CSRF_INVALID,
                        "Votre session a expiré ou la page n'est plus à jour. Rechargez la page puis réessayez.");
            } else {
                problems.write(response, HttpStatus.FORBIDDEN, ErrorCode.ADMIN_ONLY,
                        "Vous n'avez pas accès à cette fonction.");
            }
        };

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(ProtectedPaths.PATTERNS).authenticated()
                // Points d'entrée OAuth2 et pages publiques : aucune donnée sans session.
                .anyRequest().permitAll())
            .oauth2Login(oauth -> oauth
                .loginPage("/")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(new WhitelistOidcUserService(memberLoginService)))
                .failureHandler(new AccessDeniedLoginHandler())
                .defaultSuccessUrl("/", true))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .exceptionHandling(ex -> ex
                // API : 401 JSON ; pages (Swagger UI…) : retour à l'écran de connexion de la SPA.
                .authenticationEntryPoint(entryPoint(apiRequests, apiEntryPoint))
                .accessDeniedHandler(accessDenied))
            .addFilterAfter(new CurrentMemberFilter(memberRepository, problems), AuthorizationFilter.class);
        return http.build();
    }

    private static AuthenticationEntryPoint entryPoint(RequestMatcher apiRequests,
            AuthenticationEntryPoint apiEntryPoint) {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
        entryPoints.put(apiRequests, apiEntryPoint);
        DelegatingAuthenticationEntryPoint delegating = new DelegatingAuthenticationEntryPoint(entryPoints);
        delegating.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/"));
        return delegating;
    }
}
