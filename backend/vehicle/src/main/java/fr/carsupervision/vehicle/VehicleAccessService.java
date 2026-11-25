package fr.carsupervision.vehicle;

import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.member.Member;
import fr.carsupervision.member.MemberService;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Attribution des véhicules aux membres USER par un ADMIN (FR-019, FR-020). */
@Service
public class VehicleAccessService {

    static final String MEMBER_IS_ADMIN_MESSAGE =
            "Un administrateur voit déjà tous les véhicules : aucune attribution n'est nécessaire.";
    static final String UNKNOWN_VEHICLE_MESSAGE =
            "Certains véhicules sélectionnés n'existent plus. Rechargez la page puis réessayez.";

    private final VehicleAccessRepository accessRepository;
    private final VehicleRepository vehicleRepository;
    private final MemberService memberService;
    private final Clock clock;

    public VehicleAccessService(VehicleAccessRepository accessRepository, VehicleRepository vehicleRepository,
            MemberService memberService, Clock clock) {
        this.accessRepository = accessRepository;
        this.vehicleRepository = vehicleRepository;
        this.memberService = memberService;
        this.clock = clock;
    }

    /** Véhicules attribués au membre (actifs et archivés) ; 404 si le membre n'existe pas. */
    @Transactional(readOnly = true)
    public List<UUID> get(UUID memberId) {
        memberService.get(memberId);
        return accessRepository.findVehicleIdsByMemberId(memberId);
    }

    /**
     * Remplace l'ensemble des attributions du membre en une transaction. Les attributions
     * conservées gardent leur date d'origine ; seules les différences sont écrites.
     */
    @Transactional
    public List<UUID> replace(UUID memberId, Collection<UUID> vehicleIds) {
        Member member = memberService.get(memberId);
        if (member.isAdmin()) {
            throw ApiException.conflict(ErrorCode.MEMBER_IS_ADMIN, MEMBER_IS_ADMIN_MESSAGE);
        }
        Set<UUID> wanted = new HashSet<>(vehicleIds);
        if (vehicleRepository.findAllById(wanted).size() != wanted.size()) {
            throw ApiException.invalidField("vehicleIds", UNKNOWN_VEHICLE_MESSAGE);
        }

        List<VehicleAccess> current = accessRepository.findAllByMemberId(memberId);
        Set<UUID> kept = new HashSet<>();
        for (VehicleAccess access : current) {
            if (wanted.contains(access.getVehicleId())) {
                kept.add(access.getVehicleId());
            } else {
                accessRepository.delete(access);
            }
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        wanted.stream()
                .filter(vehicleId -> !kept.contains(vehicleId))
                .map(vehicleId -> new VehicleAccess(memberId, vehicleId, now))
                .forEach(accessRepository::save);
        accessRepository.flush();
        return accessRepository.findVehicleIdsByMemberId(memberId);
    }
}
