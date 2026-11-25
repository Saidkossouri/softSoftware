package fr.carsupervision.maintenance;

import fr.carsupervision.api.MaintenanceTypesApi;
import fr.carsupervision.api.model.MaintenanceTypeInput;
import fr.carsupervision.common.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Catalogue des types d'entretien : lecture pour tout membre ; écritures ADMIN à venir (US6). */
@RestController
public class MaintenanceTypesController implements MaintenanceTypesApi {

    private final MaintenanceTypeService typeService;
    private final MaintenanceTypeMapper typeMapper;

    public MaintenanceTypesController(MaintenanceTypeService typeService, MaintenanceTypeMapper typeMapper) {
        this.typeService = typeService;
        this.typeMapper = typeMapper;
    }

    @Override
    public ResponseEntity<List<fr.carsupervision.api.model.MaintenanceType>> listMaintenanceTypes(
            Boolean includeInactive, fr.carsupervision.api.model.MaintenanceTypeCategory category) {
        MaintenanceTypeCategory requested = category == null ? null : typeMapper.toEntity(category);
        return ResponseEntity.ok(typeService.list(requested, Boolean.TRUE.equals(includeInactive)).stream()
                .map(typeMapper::toDto)
                .toList());
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MaintenanceType> createMaintenanceType(
            MaintenanceTypeInput input) {
        throw notYet();
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MaintenanceType> updateMaintenanceType(UUID typeId,
            MaintenanceTypeInput input) {
        throw notYet();
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MaintenanceType> deactivateMaintenanceType(UUID typeId) {
        throw notYet();
    }

    @Override
    public ResponseEntity<fr.carsupervision.api.model.MaintenanceType> reactivateMaintenanceType(UUID typeId) {
        throw notYet();
    }

    /** Écritures du catalogue : US6. D'ici là, comme une route absente (404). */
    private static ApiException notYet() {
        return ApiException.notFound("La gestion du catalogue n'est pas encore disponible.");
    }
}
