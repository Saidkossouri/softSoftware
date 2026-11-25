package fr.carsupervision.schedule;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeadlineSuggestionDismissalRepository
        extends JpaRepository<DeadlineSuggestionDismissal, DeadlineSuggestionDismissal.Key> {

    @Query("select d.id.typeId from DeadlineSuggestionDismissal d where d.id.vehicleId = :vehicleId")
    List<UUID> findDismissedTypeIds(UUID vehicleId);
}
