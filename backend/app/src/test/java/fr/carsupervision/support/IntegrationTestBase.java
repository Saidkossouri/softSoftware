package fr.carsupervision.support;

import fr.carsupervision.member.InitialAdminInitializerAccess;
import java.util.List;
import java.util.Map;
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
 * initial ({@code said@example.com}). L'horloge partagée est fixée au 15 juin 2026 ({@link MutableClock}).
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
    protected TestVehicles vehicles;

    @Autowired
    protected MutableClock clock;

    @Autowired
    private InitialAdminInitializerAccess initialAdmin;

    /** Catalogue des types tel que pré-rempli par Liquibase, relu une fois (research R9). */
    private static List<Map<String, Object>> initialCatalogue;

    @BeforeEach
    void resetDatabase() {
        clock.reset();
        jdbc.execute("TRUNCATE maintenance_record_type, maintenance_record, mileage_reading, vehicle_access, vehicle,"
                + " member, spring_session CASCADE");
        resetCatalogue();
        initialAdmin.run();
    }

    /** Le catalogue n'est jamais vidé : types ajoutés supprimés, types pré-remplis restaurés. */
    private void resetCatalogue() {
        if (initialCatalogue == null) {
            initialCatalogue = jdbc.queryForList("SELECT * FROM maintenance_type");
        }
        List<Object> ids = initialCatalogue.stream().map(row -> row.get("id")).toList();
        jdbc.update("DELETE FROM maintenance_type WHERE id <> ALL (?)",
                (Object) ids.toArray(new java.util.UUID[0]));
        for (Map<String, Object> row : initialCatalogue) {
            jdbc.update("""
                    UPDATE maintenance_type SET name = ?, name_key = ?, default_interval_km = ?,
                      default_interval_months = ?, requires_description = ?, suggest_from_age_years = ?,
                      display_order = ?, active = ?, updated_at = ? WHERE id = ?""",
                    row.get("name"), row.get("name_key"), row.get("default_interval_km"),
                    row.get("default_interval_months"), row.get("requires_description"),
                    row.get("suggest_from_age_years"), row.get("display_order"), row.get("active"),
                    row.get("updated_at"), row.get("id"));
        }
    }
}
