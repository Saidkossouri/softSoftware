package fr.carsupervision.administration;

import fr.carsupervision.administration.member.Member;
import fr.carsupervision.administration.member.MemberRepository;
import fr.carsupervision.administration.member.Role;
import fr.carsupervision.common.Normalizers;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Garantit au démarrage l'accès de l'administrateur désigné par {@code INITIAL_ADMIN_EMAIL}
 * (FR-004, research R8). Arrête l'application si aucun administrateur ne peut exister.
 */
@Component
public class InitialAdminInitializer implements ApplicationRunner {

    static final String MISSING_MESSAGE =
            "INITIAL_ADMIN_EMAIL doit être défini tant qu'aucun administrateur n'existe.";

    private static final Logger LOG = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final MemberRepository memberRepository;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String initialAdminEmail;

    public InitialAdminInitializer(MemberRepository memberRepository, TransactionTemplate transactionTemplate,
            Clock clock, @Value("${app.initial-admin-email:}") String initialAdminEmail) {
        this.memberRepository = memberRepository;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.initialAdminEmail = initialAdminEmail;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> ensureInitialAdmin());
    }

    void ensureInitialAdmin() {
        String email = Normalizers.normalizeEmail(initialAdminEmail);
        if (email == null || email.isEmpty()) {
            if (memberRepository.countByRole(Role.ADMIN) == 0) {
                throw new IllegalStateException(MISSING_MESSAGE);
            }
            return;
        }
        if (!Normalizers.isValidEmail(email)) {
            throw new IllegalStateException(
                    "INITIAL_ADMIN_EMAIL n'est pas une adresse email valide : « " + initialAdminEmail + " ».");
        }
        memberRepository.findByEmail(email).ifPresentOrElse(member -> {
            if (!member.isAdmin()) {
                member.setRole(Role.ADMIN);
                LOG.info("Le membre désigné par INITIAL_ADMIN_EMAIL a été remis en ADMIN");
            }
        }, () -> {
            memberRepository.save(new Member(email, null, Role.ADMIN, OffsetDateTime.now(clock)));
            LOG.info("Administrateur initial ajouté à la liste blanche");
        });
    }
}
