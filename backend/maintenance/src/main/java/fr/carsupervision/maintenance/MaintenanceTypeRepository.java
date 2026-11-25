package fr.carsupervision.maintenance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTypeRepository extends JpaRepository<MaintenanceType, UUID> {

    /** Catalogue trié : Entretien puis Administratif, chacun dans son ordre d'affichage. */
    List<MaintenanceType> findAllByOrderByCategoryAscDisplayOrderAscNameAsc();

    Optional<MaintenanceType> findByNameKey(String nameKey);
}
