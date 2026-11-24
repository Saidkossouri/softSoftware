import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.14.0"
}

group = "fr.carsupervision"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val mapstructVersion = "1.6.3"
val springdocVersion = "2.8.17"
val swaggerValidatorVersion = "2.46.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.session:spring-session-jdbc")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.atlassian.oai:swagger-request-validator-mockmvc:$swaggerValidatorVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// --- Contrat d'API (principe I) : génération des interfaces et DTOs au build ---
val openApiSpec = "$projectDir/src/main/resources/openapi/openapi.yaml"
val generatedDir = layout.buildDirectory.dir("generated/openapi")

tasks.named<GenerateTask>("openApiGenerate") {
    generatorName = "spring"
    inputSpec = openApiSpec
    outputDir = generatedDir.get().asFile.path
    apiPackage = "fr.carsupervision.api"
    modelPackage = "fr.carsupervision.api.model"
    configOptions = mapOf(
        "interfaceOnly" to "true",
        "useSpringBoot3" to "true",
        "useTags" to "true",
        "skipDefaultInterface" to "true",
        "useBeanValidation" to "true",
        "openApiNullable" to "false",
        "documentationProvider" to "none",
        "annotationLibrary" to "none",
        "dateLibrary" to "java8",
        "sourceFolder" to "src/main/java",
    )
    globalProperties = mapOf("apis" to "", "models" to "", "supportingFiles" to "ApiUtil.java")
    inputs.file(openApiSpec)
}

sourceSets {
    main {
        java.srcDir(generatedDir.map { it.dir("src/main/java") })
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

// --- Frontend : embarqué dans static/ quand il a été construit (Dockerfile, CI) ---
tasks.named<ProcessResources>("processResources") {
    from("frontend/dist") {
        into("static")
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-parameters", "-Amapstruct.defaultComponentModel=spring"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// --- Lancement local : `./gradlew bootRun` lit le fichier .env et active le profil dev ---
// (Spring ne lit pas .env tout seul ; les tests et l'image Docker ne sont pas concernés.)
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "dev")
    val envFile = file(".env")
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
