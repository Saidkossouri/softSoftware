package fr.carsupervision.maintenance;

import java.util.UUID;

/**
 * Publié dans la transaction, avant la suppression de la ligne d'un entretien : le module
 * document supprime ses justificatifs (lignes, puis fichiers après validation, research R2).
 */
public record MaintenanceRecordDeleted(UUID recordId, UUID vehicleId) {
}
