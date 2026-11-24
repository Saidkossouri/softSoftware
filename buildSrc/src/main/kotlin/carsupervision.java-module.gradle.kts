// Convention de tous les modules Java du backend : Java 21, BOM Spring Boot, MapStruct, JUnit 5.
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    `java-library`
    id("io.spring.dependency-management")
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

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

val libs = versionCatalogs.named("libs")

dependencies {
    "implementation"(libs.findLibrary("mapstruct").get())
    "annotationProcessor"(libs.findLibrary("mapstruct-processor").get())
    "testImplementation"(libs.findLibrary("spring-boot-starter-test").get())
    "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Amapstruct.defaultComponentModel=spring"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
