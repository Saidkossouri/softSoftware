// Membres de la famille : entité, rôles, liste blanche et administrateur initial.
plugins {
    id("carsupervision.java-module")
}

dependencies {
    api(project(":backend:common"))
    api(libs.spring.boot.starter.data.jpa)
    // @PreAuthorize sur les opérations d'administration des membres (principe IV).
    implementation(libs.spring.security.core)
}
