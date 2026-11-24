package fr.carsupervision.administration;

import org.springframework.boot.test.context.TestComponent;

/** Permet aux tests de rejouer l'initialisation de l'ADMIN après remise à zéro de la base. */
@TestComponent
public class InitialAdminInitializerAccess {

    private final InitialAdminInitializer initializer;

    public InitialAdminInitializerAccess(InitialAdminInitializer initializer) {
        this.initializer = initializer;
    }

    public void run() {
        initializer.run(null);
    }
}
