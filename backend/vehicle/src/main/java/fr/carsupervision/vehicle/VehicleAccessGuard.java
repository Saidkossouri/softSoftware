package fr.carsupervision.vehicle;

import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.security.AuthenticatedMember;
import fr.carsupervision.security.CurrentMember;
import java.util.List;
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
    static final String ARCHIVED_MESSAGE = "Ce véhicule est archivé : aucune saisie n'est possible.";

    private final VehicleRepository vehicleRepository;
    private final CurrentMember currentMember;

    public VehicleAccessGuard(VehicleRepository vehicleRepository, CurrentMember currentMember) {
        this.vehicleRepository = vehicleRepository;
        this.currentMember = currentMember;
    }

    /** Lève 403 {@code VEHICLE_FORBIDDEN} si le membre connecté ne peut pas lire ce véhicule. */
    @Transactional(readOnly = true)
    public void assertReadable(UUID vehicleId) {
        // Un ADMIN n'est jamais refusé ici : un véhicule inexistant donne ensuite 404.
        AuthenticatedMember member = currentMember.get();
        if (!member.isAdmin() && !vehicleRepository.isActiveAndAccessibleBy(vehicleId, member.id())) {
            throw forbidden();
        }
    }

    /**
     * Prépare une écriture sur le véhicule (relevé, entretien, règle, échéance, justificatif) :
     * même refus que {@link #assertReadable} (et 404 pour un ADMIN si le véhicule n'existe pas),
     * puis 409 {@code VEHICLE_ARCHIVED} si le véhicule est archivé (ADMIN compris), puis verrou
     * de la ligne jusqu'à la fin de la transaction appelante (research R4, R5).
     */
    @Transactional
    public Vehicle lockForWrite(UUID vehicleId) {
        assertReadable(vehicleId);
        Vehicle vehicle = vehicleRepository.findByIdForUpdate(vehicleId)
                .orElseThrow(() -> ApiException.notFound("Ce véhicule n'existe pas."));
        if (!vehicle.isActive()) {
            throw ApiException.conflict(ErrorCode.VEHICLE_ARCHIVED, ARCHIVED_MESSAGE);
        }
        return vehicle;
    }

    /** Véhicules actifs visibles du membre connecté (ADMIN : tous les actifs). */
    @Transactional(readOnly = true)
    public List<Vehicle> activeAccessibleVehicles() {
        AuthenticatedMember member = currentMember.get();
        return member.isAdmin()
                ? vehicleRepository.findAllByStatusOrderByNicknameAsc(VehicleStatus.ACTIVE)
                : vehicleRepository.findActiveAccessibleBy(member.id());
    }

    /**
     * Saisie (relevé, entretien…) introuvable : 404 pour un ADMIN, mais 403 {@code VEHICLE_FORBIDDEN}
     * pour un USER afin de ne rien révéler (research R5).
     */
    public ApiException missing(String notFoundMessage) {
        return currentMember.get().isAdmin() ? ApiException.notFound(notFoundMessage) : forbidden();
    }

    static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.VEHICLE_FORBIDDEN, FORBIDDEN_MESSAGE);
    }
}
