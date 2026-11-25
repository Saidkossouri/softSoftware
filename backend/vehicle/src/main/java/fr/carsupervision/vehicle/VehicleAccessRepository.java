package fr.carsupervision.vehicle;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VehicleAccessRepository extends JpaRepository<VehicleAccess, VehicleAccessId> {

    List<VehicleAccess> findAllByMemberId(UUID memberId);

    @Query("select a.vehicleId from VehicleAccess a where a.memberId = :memberId")
    List<UUID> findVehicleIdsByMemberId(UUID memberId);

    boolean existsByMemberIdAndVehicleId(UUID memberId, UUID vehicleId);
}
