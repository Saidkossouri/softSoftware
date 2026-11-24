package fr.carsupervision.support;

import fr.carsupervision.administration.InitialAdminInitializerAccess;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base des tests d'intégration : vrai PostgreSQL (Testcontainers), Liquibase exécuté, MockMvc
 * avec la chaîne de sécurité complète. Chaque test part d'une base contenant uniquement l'ADMIN
 * initial ({@code said@example.com}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestSupportConfig.class)
public abstract class IntegrationTestBase {

    public static final String INITIAL_ADMIN = "said@example.com";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected TestMembers members;

    @Autowired
    private InitialAdminInitializerAccess initialAdmin;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("TRUNCATE vehicle_access, vehicle, member, spring_session CASCADE");
        initialAdmin.run();
    }
}
