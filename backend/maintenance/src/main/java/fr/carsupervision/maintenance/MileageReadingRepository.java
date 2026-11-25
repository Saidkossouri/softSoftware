package fr.carsupervision.maintenance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MileageReadingRepository extends JpaRepository<MileageReading, UUID> {

    /** Relevés d'un véhicule, du plus récent au plus ancien (date, puis kilométrage). */
    List<MileageReading> findByVehicleIdOrderByReadingDateDescMileageKmDesc(UUID vehicleId);

    /** Points kilométriques des relevés d'un véhicule (règle de cohérence, research R4). */
    @Query("""
            select new fr.carsupervision.maintenance.MileagePoint(r.id, r.readingDate, r.mileageKm)
            from MileageReading r where r.vehicleId = :vehicleId
            """)
    List<MileagePoint> findPoints(UUID vehicleId);

    @Query("select min(r.mileageKm) from MileageReading r where r.vehicleId = :vehicleId")
    Integer findLowestMileage(UUID vehicleId);
}
