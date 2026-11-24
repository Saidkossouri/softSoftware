package fr.carsupervision.support;

import fr.carsupervision.administration.InitialAdminInitializerAccess;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({TestMembers.class, InitialAdminInitializerAccess.class})
public class TestSupportConfig {
}
