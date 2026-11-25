package fr.carsupervision.maintenance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Entretien réalisé sur un véhicule (US2) ; son kilométrage compte comme un relevé. */
@Entity
@Table(name = "maintenance_record")
public class MaintenanceRecord {

    @Id
    private UUID id;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "mileage_km", nullable = false)
    private Integer mileageKm;

    /** Types du catalogue : peu nombreux, toujours affichés avec l'entretien. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "maintenance_record_type",
            joinColumns = @JoinColumn(name = "record_id"),
            inverseJoinColumns = @JoinColumn(name = "type_id"))
    private Set<MaintenanceType> types = new LinkedHashSet<>();

    @Column(length = 200)
    private String description;

    @Column(name = "cost_cents")
    private Integer costCents;

    @Column(length = 100)
    private String garage;

    @Column(length = 2000)
    private String notes;

    @Column(name = "author_id", updatable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 100, updatable = false)
    private String authorName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MaintenanceRecord() {
        // pour JPA
    }

    public MaintenanceRecord(UUID vehicleId, UUID authorId, String authorName, OffsetDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.vehicleId = vehicleId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(LocalDate recordDate, int mileageKm, Collection<MaintenanceType> types, String description,
            Integer costCents, String garage, String notes) {
        this.recordDate = recordDate;
        this.mileageKm = mileageKm;
        this.types.clear();
        this.types.addAll(types);
        this.description = description;
        this.costCents = costCents;
        this.garage = garage;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public Integer getMileageKm() {
        return mileageKm;
    }

    public Set<MaintenanceType> getTypes() {
        return types;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCostCents() {
        return costCents;
    }

    public String getGarage() {
        return garage;
    }

    public String getNotes() {
        return notes;
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
