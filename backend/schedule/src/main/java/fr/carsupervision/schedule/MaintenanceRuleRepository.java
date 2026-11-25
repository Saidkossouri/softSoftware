package fr.carsupervision.schedule;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRuleRepository extends JpaRepository<MaintenanceRule, UUID> {

    List<MaintenanceRule> findByVehicleId(UUID vehicleId);

    boolean existsByVehicleIdAndTypeId(UUID vehicleId, UUID typeId);
}
