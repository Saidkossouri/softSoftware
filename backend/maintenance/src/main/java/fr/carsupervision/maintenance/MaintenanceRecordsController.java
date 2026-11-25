package fr.carsupervision.maintenance;

import fr.carsupervision.api.MaintenanceApi;
import fr.carsupervision.api.model.MaintenanceRecordInput;
import fr.carsupervision.api.model.VehicleHistory;
import fr.carsupervision.common.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Entretiens réalisés : droits et validation dans {@link MaintenanceRecordService}. */
@RestController
public class MaintenanceRecordsController implements MaintenanceApi {

    private final MaintenanceRecordService recordService;
    private final MaintenanceRecordMapper recordMapper;

    public MaintenanceRecordsController(MaintenanceRecordService recordService, MaintenanceRecordMapper recordMapper) {
        this.recordService = recordService;
        this.recordMapper = recordMapper;
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

    /** Historique du véhicule : US3. D'ici là, comme une route absente (404). */
    @Override
    public ResponseEntity<VehicleHistory> getVehicleHistory(UUID vehicleId, UUID typeId, Boolean recordsOnly) {
        throw ApiException.notFound("L'historique n'est pas encore disponible.");
    }
}
