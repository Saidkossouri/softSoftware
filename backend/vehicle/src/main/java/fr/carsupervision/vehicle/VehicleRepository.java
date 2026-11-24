package fr.carsupervision.vehicle;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findAllByStatusOrderByNicknameAsc(VehicleStatus status);

    boolean existsByRegistrationKey(String registrationKey);

    boolean existsByRegistrationKeyAndIdNot(String registrationKey, UUID id);
}
