package fr.carsupervision.maintenance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

    /** Points kilométriques des entretiens d'un véhicule (research R4). */
    @Query("""
            select new fr.carsupervision.maintenance.MileagePoint(r.id, r.recordDate, r.mileageKm)
            from MaintenanceRecord r where r.vehicleId = :vehicleId
            """)
    List<MileagePoint> findPoints(UUID vehicleId);

    @Query("select min(r.mileageKm) from MaintenanceRecord r where r.vehicleId = :vehicleId")
    Integer findLowestMileage(UUID vehicleId);
}
