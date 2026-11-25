package fr.carsupervision.support;

import fr.carsupervision.member.InitialAdminInitializerAccess;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@Import({TestMembers.class, TestVehicles.class, InitialAdminInitializerAccess.class})
public class TestSupportConfig {

    /** Remplace l'horloge système partagée ({@code ClockConfig}) dans les tests d'intégration. */
    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock();
    }
}
