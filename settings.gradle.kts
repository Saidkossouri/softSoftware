plugins {
    // Télécharge automatiquement le JDK exigé par la toolchain (Java 21) s'il n'est pas installé.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "carsupervision"

// Monolithe modulaire : un seul déployable (backend/app), un sous-projet Gradle par module.
include(
    "backend:api-contract", // contrat OpenAPI, interfaces et DTOs générés
    "backend:common",       // erreurs Problem Details, normalisation, configuration web
    "backend:member",       // membres, rôles, liste blanche, administrateur initial
    "backend:security",     // connexion Google, session, membre connecté
    "backend:vehicle",      // parc de véhicules et attributions
    "backend:maintenance",  // relevés de kilométrage, entretiens, catalogue des types
    "backend:schedule",     // échéances, rappels et suggestions
    "backend:document",     // justificatifs des entretiens et leur stockage
    "backend:app",          // point d'entrée Spring Boot, configuration, tests d'intégration
)
