package fr.carsupervision.schedule;

import fr.carsupervision.maintenance.MaintenanceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Règle d'échéance d'un véhicule : une opération, une fréquence et un point de départ (FR-021). */
@Entity
@Table(name = "maintenance_rule")
public class MaintenanceRule {

    @Id
    private UUID id;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "type_id", nullable = false, updatable = false)
    private MaintenanceType type;

    @Column(name = "interval_km")
    private Integer intervalKm;

    @Column(name = "interval_months")
    private Short intervalMonths;

    @Column(name = "baseline_date", nullable = false)
    private LocalDate baselineDate;

    @Column(name = "baseline_mileage_km", nullable = false)
    private Integer baselineMileageKm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected MaintenanceRule() {
    }

    public MaintenanceRule(UUID vehicleId, MaintenanceType type, OffsetDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.vehicleId = vehicleId;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(Integer intervalKm, Integer intervalMonths, LocalDate baselineDate, int baselineMileageKm,
            OffsetDateTime updatedAt) {
        this.intervalKm = intervalKm;
        this.intervalMonths = intervalMonths == null ? null : intervalMonths.shortValue();
        this.baselineDate = baselineDate;
        this.baselineMileageKm = baselineMileageKm;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public MaintenanceType getType() {
        return type;
    }

    public Integer getIntervalKm() {
        return intervalKm;
    }

    public Integer getIntervalMonths() {
        return intervalMonths == null ? null : intervalMonths.intValue();
    }

    public LocalDate getBaselineDate() {
        return baselineDate;
    }

    public int getBaselineMileageKm() {
        return baselineMileageKm;
    }
}
