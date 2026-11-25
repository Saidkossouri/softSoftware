package fr.carsupervision.vehicle;

import fr.carsupervision.api.VehicleAccessApi;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Attributions des véhicules aux membres : réservées aux ADMIN, contrôlées côté serveur. */
@RestController
public class VehicleAccessController implements VehicleAccessApi {

    private final VehicleAccessService accessService;
    private final VehicleMapper vehicleMapper;

    public VehicleAccessController(VehicleAccessService accessService, VehicleMapper vehicleMapper) {
        this.accessService = accessService;
        this.vehicleMapper = vehicleMapper;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.VehicleAccess> getMemberVehicleAccess(UUID memberId) {
        return ResponseEntity.ok(vehicleMapper.toAccessDto(accessService.get(memberId)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.VehicleAccess> replaceMemberVehicleAccess(UUID memberId,
            fr.carsupervision.api.model.VehicleAccess request) {
        return ResponseEntity.ok(vehicleMapper.toAccessDto(
                accessService.replace(memberId, request.getVehicleIds())));
    }
}
