package fr.carsupervision.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Justificatif (facture, photo) rattaché à un entretien ; le fichier est dans {@link DocumentStorage}. */
@Entity
@Table(name = "document")
public class Document {

    @Id
    private UUID id;

    @Column(name = "record_id", nullable = false, updatable = false)
    private UUID recordId;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @Column(name = "storage_key", nullable = false, updatable = false, length = 100)
    private String storageKey;

    @Column(name = "file_name", nullable = false, length = 120)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "author_id", updatable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 100, updatable = false)
    private String authorName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Document() {
    }

    public Document(UUID recordId, UUID vehicleId, String fileName, String contentType, long sizeBytes,
            UUID authorId, String authorName, OffsetDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.recordId = recordId;
        this.vehicleId = vehicleId;
        this.storageKey = vehicleId + "/" + id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecordId() {
        return recordId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
