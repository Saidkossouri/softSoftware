// Connexion Google (OAuth2 / OIDC), liste blanche à la connexion, session, membre connecté.
plugins {
    id("carsupervision.java-module")
}

dependencies {
    api(project(":backend:member"))
    api(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.session.jdbc)

    testImplementation(libs.spring.security.test)
}
