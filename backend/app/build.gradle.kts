// Le déployable unique : assemble les modules, porte la configuration, Liquibase et les tests d'intégration.
plugins {
    id("carsupervision.java-module")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":backend:common"))
    implementation(project(":backend:member"))
    implementation(project(":backend:security"))
    implementation(project(":backend:vehicle"))
    implementation(project(":backend:maintenance"))
    implementation(project(":backend:schedule"))
    implementation(project(":backend:document"))

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.liquibase.core)
    implementation(libs.springdoc.openapi.ui)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.starter.oauth2.client)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.swagger.request.validator.mockmvc)
}

// Nom stable du jar exécutable (utilisé par le Dockerfile).
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "carsupervision.jar"
}

// Lancement local : `./gradlew bootRun` lit le fichier .env à la racine et active le profil dev
// (Spring ne lit pas .env tout seul ; les tests et l'image Docker ne sont pas concernés).
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "dev")
    workingDir = rootProject.projectDir
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines(Charsets.UTF_8)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val (key, value) = line.split("=", limit = 2)
                environment(key.trim(), value.trim())
            }
    }
}
