package fr.carsupervision.maintenance;

import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.security.CurrentMember;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecture du catalogue des types d'entretien ; les écritures (ADMIN) arrivent avec l'US6. */
@Service
public class MaintenanceTypeService {

    private final MaintenanceTypeRepository typeRepository;
    private final CurrentMember currentMember;

    public MaintenanceTypeService(MaintenanceTypeRepository typeRepository, CurrentMember currentMember) {
        this.typeRepository = typeRepository;
        this.currentMember = currentMember;
    }

    /**
     * Types du catalogue, actifs seulement sauf {@code includeInactive} (réservé aux ADMIN, 403
     * {@code ADMIN_ONLY} sinon), éventuellement d'une seule catégorie.
     */
    @Transactional(readOnly = true)
    public List<MaintenanceType> list(MaintenanceTypeCategory category, boolean includeInactive) {
        if (includeInactive && !currentMember.get().isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ADMIN_ONLY,
                    "Seul un administrateur peut voir les types désactivés.");
        }
        // MAINTENANCE avant ADMINISTRATIVE : l'ordre alphabétique des noms d'énumération convient.
        return typeRepository.findAllByOrderByCategoryAscDisplayOrderAscNameAsc().stream()
                .filter(type -> includeInactive || type.isActive())
                .filter(type -> category == null || type.getCategory() == category)
                .sorted(java.util.Comparator.comparing((MaintenanceType type) -> type.getCategory().ordinal())
                        .thenComparingInt(MaintenanceType::getDisplayOrder)
                        .thenComparing(MaintenanceType::getName))
                .toList();
    }
}
