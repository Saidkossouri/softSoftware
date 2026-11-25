package fr.carsupervision.maintenance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Relevé kilométrique d'un véhicule (US1). */
@Entity
@Table(name = "mileage_reading")
public class MileageReading {

    @Id
    private UUID id;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "mileage_km", nullable = false)
    private Integer mileageKm;

    @Column(length = 200)
    private String note;

    /** Auteur ; vide si le membre a été retiré de la liste blanche (research R6). */
    @Column(name = "author_id", updatable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 100, updatable = false)
    private String authorName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MileageReading() {
        // pour JPA
    }

    public MileageReading(UUID vehicleId, UUID authorId, String authorName, OffsetDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.vehicleId = vehicleId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public LocalDate getReadingDate() {
        return readingDate;
    }

    public Integer getMileageKm() {
        return mileageKm;
    }

    public String getNote() {
        return note;
    }

    public void update(LocalDate readingDate, int mileageKm, String note) {
        this.readingDate = readingDate;
        this.mileageKm = mileageKm;
        this.note = note;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
