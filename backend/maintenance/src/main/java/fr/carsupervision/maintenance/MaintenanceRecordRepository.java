package fr.carsupervision.maintenance;

import java.time.LocalDate;
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

    List<MaintenanceRecord> findByVehicleId(UUID vehicleId);

    /** Entretiens d'un véhicule comportant un type donné (filtre FR-016). */
    @Query("""
            select distinct r from MaintenanceRecord r join r.types t
            where r.vehicleId = :vehicleId and t.id = :typeId
            """)
    List<MaintenanceRecord> findByVehicleIdAndType(UUID vehicleId, UUID typeId);

    /** Somme des coûts saisis, en centimes ; les entretiens sans coût ne comptent pas (FR-015). */
    @Query("select coalesce(sum(r.costCents), 0) from MaintenanceRecord r where r.vehicleId = :vehicleId")
    long sumCost(UUID vehicleId);

    @Query("""
            select coalesce(sum(r.costCents), 0) from MaintenanceRecord r
            where r.vehicleId = :vehicleId and r.recordDate between :from and :to
            """)
    long sumCostBetween(UUID vehicleId, LocalDate from, LocalDate to);

    /** Réalisations d'un type sur un véhicule, la plus récente d'abord (date, puis kilométrage). */
    @Query("""
            select new fr.carsupervision.maintenance.MileagePoint(r.id, r.recordDate, r.mileageKm)
            from MaintenanceRecord r join r.types t
            where r.vehicleId = :vehicleId and t.id = :typeId
            order by r.recordDate desc, r.mileageKm desc
            """)
    List<MileagePoint> findOccurrences(UUID vehicleId, UUID typeId, org.springframework.data.domain.Limit limit);
}
