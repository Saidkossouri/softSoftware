// Parc de véhicules et attributions aux membres (histoires 3 et 4 de la fonctionnalité 001).
// Pour l'instant, le module porte uniquement les migrations de ses tables.
plugins {
    id("carsupervision.java-module")
}

dependencies {
    api(project(":backend:security"))
}
