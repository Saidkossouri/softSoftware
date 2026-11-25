package fr.carsupervision.maintenance;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Port implémenté par le module document (research R2) : nombre de justificatifs par entretien.
 * Un entretien absent de la réponse en a 0 ; sans module document, tous en ont 0.
 */
public interface RecordDocumentCounter {

    Map<UUID, Integer> countByRecord(Collection<UUID> recordIds);
}
