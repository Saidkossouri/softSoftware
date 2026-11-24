package fr.carsupervision.vehicle;

import fr.carsupervision.api.VehiclesApi;
import fr.carsupervision.api.model.VehicleInput;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * Parc de véhicules. Les écritures sont réservées aux ADMIN côté serveur ; la lecture est
 * filtrée par le service selon le membre connecté.
 */
@RestController
public class VehiclesController implements VehiclesApi {

    private final VehicleService vehicleService;
    private final VehicleMapper vehicleMapper;

    public VehiclesController(VehicleService vehicleService, VehicleMapper vehicleMapper) {
        this.vehicleService = vehicleService;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    public ResponseEntity<List<fr.carsupervision.api.model.Vehicle>> listVehicles(
            fr.carsupervision.api.model.VehicleStatus status) {
        VehicleStatus requested = status == null ? null : vehicleMapper.toEntity(status);
        return ResponseEntity.ok(vehicleService.list(requested).stream().map(vehicleMapper::toDto).toList());
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.Vehicle> getVehicle(UUID vehicleId) {
        return ResponseEntity.ok(vehicleMapper.toDto(vehicleService.get(vehicleId)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.Vehicle> createVehicle(VehicleInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleMapper.toDto(vehicleService.create(input)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.Vehicle> updateVehicle(UUID vehicleId, VehicleInput input) {
        return ResponseEntity.ok(vehicleMapper.toDto(vehicleService.update(vehicleId, input)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.Vehicle> archiveVehicle(UUID vehicleId) {
        return ResponseEntity.ok(vehicleMapper.toDto(vehicleService.archive(vehicleId)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.Vehicle> reactivateVehicle(UUID vehicleId) {
        return ResponseEntity.ok(vehicleMapper.toDto(vehicleService.reactivate(vehicleId)));
    }
}
