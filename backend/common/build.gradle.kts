// Socle technique partagé : erreurs Problem Details, normalisation, horloge, configuration web.
plugins {
    id("carsupervision.java-module")
}

dependencies {
    api(project(":backend:api-contract"))
    // Exceptions traduites en Problem Details (conflit d'intégrité, accès refusé).
    implementation(libs.spring.tx)
    implementation(libs.spring.security.core)
}
