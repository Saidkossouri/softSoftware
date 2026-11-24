// Contrat d'API (constitution, principe I) : openapi.yaml et génération des interfaces *Api et DTOs.
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("carsupervision.java-module")
    id("org.openapi.generator")
}

dependencies {
    // Types utilisés par le code généré, exposés aux modules qui implémentent le contrat.
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
}

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
