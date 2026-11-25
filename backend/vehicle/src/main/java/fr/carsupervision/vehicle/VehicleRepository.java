package fr.carsupervision.vehicle;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findAllByStatusOrderByNicknameAsc(VehicleStatus status);

    /** Véhicules actifs attribués au membre, triés par surnom (vue « Mes véhicules » d'un USER). */
    @Query("""
            select v from Vehicle v
            where v.status = fr.carsupervision.vehicle.VehicleStatus.ACTIVE
              and exists (select 1 from VehicleAccess a where a.vehicleId = v.id and a.memberId = :memberId)
            order by v.nickname asc
            """)
    List<Vehicle> findActiveAccessibleBy(UUID memberId);

    /** Vrai si le véhicule existe, est actif et est attribué au membre. */
    @Query("""
            select count(v) > 0 from Vehicle v
            where v.id = :vehicleId
              and v.status = fr.carsupervision.vehicle.VehicleStatus.ACTIVE
              and exists (select 1 from VehicleAccess a where a.vehicleId = v.id and a.memberId = :memberId)
            """)
    boolean isActiveAndAccessibleBy(UUID vehicleId, UUID memberId);

    boolean existsByRegistrationKey(String registrationKey);

    boolean existsByRegistrationKeyAndIdNot(String registrationKey, UUID id);
}
