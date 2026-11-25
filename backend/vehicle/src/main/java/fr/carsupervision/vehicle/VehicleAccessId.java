package fr.carsupervision.vehicle;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clé composée d'une attribution : membre + véhicule. */
public class VehicleAccessId implements Serializable {

    private UUID memberId;
    private UUID vehicleId;

    protected VehicleAccessId() {
        // pour JPA
    }

    public VehicleAccessId(UUID memberId, UUID vehicleId) {
        this.memberId = memberId;
        this.vehicleId = vehicleId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VehicleAccessId that
                && Objects.equals(memberId, that.memberId)
                && Objects.equals(vehicleId, that.vehicleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, vehicleId);
    }
}
