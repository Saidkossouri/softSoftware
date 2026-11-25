package fr.carsupervision.maintenance;

/** Catégorie d'un type du catalogue (research R9). */
public enum MaintenanceTypeCategory {
    /** Opération d'un entretien, point d'accroche des règles (fréquence en km et/ou en mois). */
    MAINTENANCE,
    /** Modèle d'échéance à date fixe : assurance, vignette, visite technique. */
    ADMINISTRATIVE
}
