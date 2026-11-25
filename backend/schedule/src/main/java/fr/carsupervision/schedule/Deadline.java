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

/** Échéance à date fixe (assurance, vignette, visite technique…), éventuellement récurrente. */
@Entity
@Table(name = "deadline")
public class Deadline {

    @Id
    private UUID id;

    @Column(name = "vehicle_id", nullable = false, updatable = false)
    private UUID vehicleId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id")
    private MaintenanceType type;

    @Column(nullable = false, length = 60)
    private String label;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "recurrence_months")
    private Short recurrenceMonths;

    @Column(name = "done_at")
    private OffsetDateTime doneAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Deadline() {
    }

    public Deadline(UUID vehicleId, OffsetDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.vehicleId = vehicleId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void update(MaintenanceType type, String label, LocalDate dueDate, Integer recurrenceMonths,
            OffsetDateTime updatedAt) {
        this.type = type;
        this.label = label;
        this.dueDate = dueDate;
        this.recurrenceMonths = recurrenceMonths == null ? null : recurrenceMonths.shortValue();
        this.updatedAt = updatedAt;
    }

    /** Échéance suivante d'une échéance récurrente : même libellé, même récurrence. */
    public Deadline next(OffsetDateTime now) {
        Deadline next = new Deadline(vehicleId, now);
        next.update(type, label, dueDate.plusMonths(recurrenceMonths), getRecurrenceMonths(), now);
        return next;
    }

    public void markDone(OffsetDateTime now) {
        this.doneAt = now;
        this.updatedAt = now;
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

    public String getLabel() {
        return label;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Integer getRecurrenceMonths() {
        return recurrenceMonths == null ? null : recurrenceMonths.intValue();
    }

    public OffsetDateTime getDoneAt() {
        return doneAt;
    }

    public boolean isDone() {
        return doneAt != null;
    }
}
