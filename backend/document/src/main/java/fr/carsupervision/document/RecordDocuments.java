package fr.carsupervision.document;

import fr.carsupervision.maintenance.MaintenanceRecordDeleted;
import fr.carsupervision.maintenance.RecordDocumentCounter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Lien avec le module maintenance (research R2) : nombre de justificatifs par entretien (port
 * {@link RecordDocumentCounter}) et suppression des justificatifs d'un entretien supprimé (lignes
 * dans la transaction, fichiers après validation).
 */
@Component
public class RecordDocuments implements RecordDocumentCounter {

    private final DocumentRepository documentRepository;
    private final DocumentStorage storage;

    public RecordDocuments(DocumentRepository documentRepository, DocumentStorage storage) {
        this.documentRepository = documentRepository;
        this.storage = storage;
    }

    @Override
    public Map<UUID, Integer> countByRecord(Collection<UUID> recordIds) {
        Map<UUID, Integer> counts = new HashMap<>();
        if (recordIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : documentRepository.countByRecordIds(recordIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    @EventListener
    public void onRecordDeleted(MaintenanceRecordDeleted event) {
        List<Document> documents = documentRepository.findByRecordIdOrderByCreatedAtAscFileNameAsc(event.recordId());
        if (documents.isEmpty()) {
            return;
        }
        documentRepository.deleteAll(documents);
        documentRepository.flush();
        List<String> keys = documents.stream().map(Document::getStorageKey).toList();
        AfterCommit.run(() -> keys.forEach(storage::delete));
    }
}
