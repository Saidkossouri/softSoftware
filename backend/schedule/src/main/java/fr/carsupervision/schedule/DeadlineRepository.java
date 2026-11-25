package fr.carsupervision.schedule;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadlineRepository extends JpaRepository<Deadline, UUID> {

    /** Échéances à faire d'un véhicule. */
    List<Deadline> findByVehicleIdAndDoneAtIsNull(UUID vehicleId);
}
