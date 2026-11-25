package fr.carsupervision.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Suggestion d'échéance ignorée pour un véhicule (FR-040) : elle ne revient plus. */
@Entity
@Table(name = "deadline_suggestion_dismissal")
public class DeadlineSuggestionDismissal {

    @Embeddable
    public record Key(@Column(name = "vehicle_id") UUID vehicleId, @Column(name = "type_id") UUID typeId)
            implements Serializable {
    }

    @EmbeddedId
    private Key id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected DeadlineSuggestionDismissal() {
    }

    public DeadlineSuggestionDismissal(Key id, OffsetDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public Key getId() {
        return id;
    }
}
