package fr.carsupervision.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.carsupervision.member.Member;
import fr.carsupervision.member.MemberRepository;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** FR-004 et cas limites de INITIAL_ADMIN_EMAIL (research R8). */
class InitialAdminInitializerIT extends IntegrationTestBase {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Clock clock;

    private InitialAdminInitializer initializerWith(String email) {
        return new InitialAdminInitializer(memberRepository, transactionTemplate, clock, email);
    }

    @Test
    void createsInitialAdminWithNormalizedEmail() {
        // La configuration de test contient « Said@Example.com » avec des espaces.
        assertThat(memberRepository.findAll())
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.getEmail()).isEqualTo(INITIAL_ADMIN);
                    assertThat(member.getRole()).isEqualTo(Role.ADMIN);
                    assertThat(member.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void isIdempotentOnRestart() {
        initializerWith(INITIAL_ADMIN).run(null);
        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    void promotesExistingUserBackToAdmin() {
        members.create("epouse@example.com", Role.USER);

        initializerWith(" EPOUSE@example.com").run(null);

        assertThat(memberRepository.findByEmail("epouse@example.com"))
                .get().extracting(Member::getRole).isEqualTo(Role.ADMIN);
    }

    @Test
    void keepsPreviousInitialAdminWhenVariableChanges() {
        initializerWith("nouveau@example.com").run(null);

        assertThat(memberRepository.findByEmail(INITIAL_ADMIN))
                .get().extracting(Member::getRole).isEqualTo(Role.ADMIN);
        assertThat(memberRepository.findByEmail("nouveau@example.com"))
                .get().extracting(Member::getRole).isEqualTo(Role.ADMIN);
    }

    @Test
    void refusesToStartWithoutVariableWhenNoAdminExists() {
        jdbc.execute("TRUNCATE member CASCADE");

        assertThatThrownBy(() -> initializerWith("").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(InitialAdminInitializer.MISSING_MESSAGE);
        assertThatThrownBy(() -> initializerWith(null).run(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startsWithoutVariableWhenAnAdminAlreadyExists() {
        initializerWith("  ").run(null);
        assertThat(memberRepository.countByRole(Role.ADMIN)).isEqualTo(1);
    }

    @Test
    void refusesMalformedEmail() {
        assertThatThrownBy(() -> initializerWith("pas-un-email").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INITIAL_ADMIN_EMAIL");
    }
}
