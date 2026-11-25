package fr.carsupervision.vehicle;

import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.security.AuthenticatedMember;
import fr.carsupervision.security.CurrentMember;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Règle de lecture d'un véhicule (FR-021, FR-022), réutilisable par les futurs modules
 * (entretiens, échéances) : un ADMIN lit tout ; un USER lit uniquement ses véhicules actifs
 * attribués. Pour un USER, un véhicule inexistant, archivé ou non attribué donne le même refus
 * afin de ne rien révéler.
 */
@Component
public class VehicleAccessGuard {

    static final String FORBIDDEN_MESSAGE = "Vous n'avez pas accès à ce véhicule.";

    private final VehicleRepository vehicleRepository;
    private final CurrentMember currentMember;

    public VehicleAccessGuard(VehicleRepository vehicleRepository, CurrentMember currentMember) {
        this.vehicleRepository = vehicleRepository;
        this.currentMember = currentMember;
    }

    /** Lève 403 {@code VEHICLE_FORBIDDEN} si le membre connecté ne peut pas lire ce véhicule. */
    @Transactional(readOnly = true)
    public void assertReadable(UUID vehicleId) {
        AuthenticatedMember member = currentMember.get();
        if (!member.isAdmin() && !vehicleRepository.isActiveAndAccessibleBy(vehicleId, member.id())) {
            throw forbidden();
        }
    }

    static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.VEHICLE_FORBIDDEN, FORBIDDEN_MESSAGE);
    }
}
