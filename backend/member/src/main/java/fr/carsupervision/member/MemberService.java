package fr.carsupervision.member;

import fr.carsupervision.api.model.MemberCreateRequest;
import fr.carsupervision.api.model.MemberUpdateRequest;
import fr.carsupervision.common.Normalizers;
import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestion de la liste blanche : ajout, modification et retrait des membres (FR-010 à FR-012). */
@Service
public class MemberService {

    static final String LAST_ADMIN_MESSAGE =
            "Il doit toujours rester au moins un administrateur. Nommez d'abord un autre administrateur.";

    private static final Comparator<Member> BY_NAME_THEN_EMAIL = Comparator
            .comparing((Member m) -> m.getDisplayName() == null
                    ? null : m.getDisplayName().toLowerCase(Locale.ROOT),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Member::getEmail);

    private final MemberRepository memberRepository;
    private final Clock clock;

    public MemberService(MemberRepository memberRepository, Clock clock) {
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Member> list() {
        return memberRepository.findAll().stream().sorted(BY_NAME_THEN_EMAIL).toList();
    }

    @Transactional(readOnly = true)
    public Member get(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Ce membre n'existe pas ou a été retiré."));
    }

    @Transactional
    public Member create(MemberCreateRequest request) {
        String email = Normalizers.normalizeEmail(request.getEmail());
        if (!Normalizers.isValidEmail(email)) {
            throw ApiException.invalidField("email", "Saisissez une adresse email valide.");
        }
        if (memberRepository.existsByEmail(email)) {
            throw ApiException.conflict(ErrorCode.EMAIL_ALREADY_EXISTS,
                    "Cet email est déjà dans la liste des membres.");
        }
        Member member = new Member(email, Normalizers.blankToNull(request.getDisplayName()),
                Role.valueOf(request.getRole().getValue()), OffsetDateTime.now(clock));
        return memberRepository.saveAndFlush(member);
    }

    /** Champs absents inchangés ; un nom affiché vide efface le nom. */
    @Transactional
    public Member update(UUID id, MemberUpdateRequest request) {
        if (request.getDisplayName() == null && request.getRole() == null) {
            throw ApiException.invalidField("role", "Indiquez au moins une modification.");
        }
        Member member = get(id);
        if (request.getRole() != null) {
            Role newRole = Role.valueOf(request.getRole().getValue());
            if (member.isAdmin() && newRole == Role.USER) {
                ensureAnotherAdminRemains(member);
            }
            member.setRole(newRole);
        }
        if (request.getDisplayName() != null) {
            member.setDisplayName(Normalizers.blankToNull(request.getDisplayName()));
        }
        return member;
    }

    /** Retire le membre ; ses attributions de véhicules sont supprimées en cascade par la base. */
    @Transactional
    public void delete(UUID id) {
        Member member = get(id);
        if (member.isAdmin()) {
            ensureAnotherAdminRemains(member);
        }
        memberRepository.delete(member);
        memberRepository.flush();
    }

    /**
     * Verrouille les lignes ADMIN ({@code FOR UPDATE}) avant de compter : deux retraits ou
     * rétrogradations simultanés sont sérialisés et le second voit l'état réel (research R9).
     */
    private void ensureAnotherAdminRemains(Member admin) {
        boolean anotherAdmin = memberRepository.findAllByRoleForUpdate(Role.ADMIN).stream()
                .anyMatch(other -> !other.getId().equals(admin.getId()));
        if (!anotherAdmin) {
            throw ApiException.conflict(ErrorCode.LAST_ADMIN, LAST_ADMIN_MESSAGE);
        }
    }
}
