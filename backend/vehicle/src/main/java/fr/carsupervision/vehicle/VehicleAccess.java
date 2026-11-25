package fr.carsupervision.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Attribution d'un véhicule à un membre USER (FR-019, FR-020). Conservée pendant l'archivage
 * du véhicule ; supprimée en cascade par la base quand le membre est retiré.
 */
@Entity
@Table(name = "vehicle_access")
@IdClass(VehicleAccessId.class)
public class VehicleAccess {

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @Id
    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt;

    protected VehicleAccess() {
        // pour JPA
    }

    public VehicleAccess(UUID memberId, UUID vehicleId, OffsetDateTime grantedAt) {
        this.memberId = memberId;
        this.vehicleId = vehicleId;
        this.grantedAt = grantedAt;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public UUID getVehicleId() {
        return vehicleId;
    }

    public OffsetDateTime getGrantedAt() {
        return grantedAt;
    }
}
