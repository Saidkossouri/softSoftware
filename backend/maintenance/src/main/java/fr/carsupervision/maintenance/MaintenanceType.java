package fr.carsupervision.maintenance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Type du catalogue géré par l'ADMIN (US6, FR-036 à FR-040). Jamais supprimé, seulement désactivé. */
@Entity
@Table(name = "maintenance_type")
public class MaintenanceType {

    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String name;

    /** Clé d'unicité : minuscules, espaces internes réduits. */
    @Column(name = "name_key", nullable = false, unique = true, length = 60)
    private String nameKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15, updatable = false)
    private MaintenanceTypeCategory category;

    @Column(name = "default_interval_km")
    private Integer defaultIntervalKm;

    @Column(name = "default_interval_months")
    private Short defaultIntervalMonths;

    @Column(name = "requires_description", nullable = false)
    private boolean requiresDescription;

    @Column(name = "suggest_from_age_years")
    private Short suggestFromAgeYears;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MaintenanceType() {
        // pour JPA
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNameKey() {
        return nameKey;
    }

    public MaintenanceTypeCategory getCategory() {
        return category;
    }

    public Integer getDefaultIntervalKm() {
        return defaultIntervalKm;
    }

    public Integer getDefaultIntervalMonths() {
        return defaultIntervalMonths == null ? null : defaultIntervalMonths.intValue();
    }

    public boolean isRequiresDescription() {
        return requiresDescription;
    }

    public Integer getSuggestFromAgeYears() {
        return suggestFromAgeYears == null ? null : suggestFromAgeYears.intValue();
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
