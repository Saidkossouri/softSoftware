plugins {
    // Télécharge automatiquement le JDK exigé par la toolchain (Java 21) s'il n'est pas installé.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "carsupervision"
