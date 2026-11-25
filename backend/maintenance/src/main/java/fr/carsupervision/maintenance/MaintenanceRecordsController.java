package fr.carsupervision.maintenance;

import fr.carsupervision.api.MaintenanceApi;
import fr.carsupervision.api.model.MaintenanceRecordInput;
import fr.carsupervision.api.model.VehicleHistory;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Entretiens réalisés et historique du véhicule : droits et validation dans les services. */
@RestController
public class MaintenanceRecordsController implements MaintenanceApi {

    private final MaintenanceRecordService recordService;
    private final MaintenanceRecordMapper recordMapper;
    private final VehicleHistoryService historyService;
    private final VehicleHistoryMapper historyMapper;

    public MaintenanceRecordsController(MaintenanceRecordService recordService, MaintenanceRecordMapper recordMapper,
            VehicleHistoryService historyService, VehicleHistoryMapper historyMapper) {
        this.recordService = recordService;
        this.recordMapper = recordMapper;
        this.historyService = historyService;
        this.historyMapper = historyMapper;
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MaintenanceRecord> createMaintenanceRecord(UUID vehicleId,
            MaintenanceRecordInput input) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordMapper.toDto(recordService.create(vehicleId, input)));
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MaintenanceRecord> getMaintenanceRecord(UUID recordId) {
        return ResponseEntity.ok(recordMapper.toDto(recordService.get(recordId)));
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MaintenanceRecord> updateMaintenanceRecord(UUID recordId,
            MaintenanceRecordInput input) {
        return ResponseEntity.ok(recordMapper.toDto(recordService.update(recordId, input)));
    }

    @Override
    public ResponseEntity<Void> deleteMaintenanceRecord(UUID recordId) {
        recordService.delete(recordId);
        return ResponseEntity.noContent().build();
    }

    /** Historique du véhicule (US3) : le contrat le range sous le tag maintenance, d'où ce contrôleur. */
    @Override
    public ResponseEntity<VehicleHistory> getVehicleHistory(UUID vehicleId, UUID typeId, Boolean recordsOnly) {
        return ResponseEntity.ok(historyMapper.toDto(
                historyService.get(vehicleId, typeId, Boolean.TRUE.equals(recordsOnly))));
    }
}
