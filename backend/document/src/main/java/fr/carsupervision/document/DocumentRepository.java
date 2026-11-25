package fr.carsupervision.document;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByRecordIdOrderByCreatedAtAscFileNameAsc(UUID recordId);

    long countByRecordId(UUID recordId);

    /** Nombre de justificatifs par entretien : paires (identifiant d'entretien, nombre). */
    @Query("""
            select d.recordId, count(d) from Document d
            where d.recordId in :recordIds group by d.recordId
            """)
    List<Object[]> countByRecordIds(Collection<UUID> recordIds);
}
