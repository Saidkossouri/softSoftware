package fr.carsupervision.security;

import fr.carsupervision.member.Member;
import fr.carsupervision.member.MemberRepository;
import fr.carsupervision.common.Normalizers;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Décide si un compte Google peut se connecter et met à jour la fiche du membre (research R3). */
@Service
public class MemberLoginService {

    private final MemberRepository memberRepository;
    private final Clock clock;

    public MemberLoginService(MemberRepository memberRepository, Clock clock) {
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    /**
     * @return le membre autorisé, ou vide si l'email n'est pas vérifié par Google ou absent de la
     *     liste blanche
     */
    @Transactional
    public Optional<Member> recordLogin(String email, Boolean emailVerified, String googleName) {
        if (!Boolean.TRUE.equals(emailVerified)) {
            return Optional.empty();
        }
        String normalized = Normalizers.normalizeEmail(email);
        if (normalized == null) {
            return Optional.empty();
        }
        return memberRepository.findByEmail(normalized).map(member -> {
            member.setLastLoginAt(OffsetDateTime.now(clock));
            if (member.getDisplayName() == null) {
                member.setDisplayName(truncate(Normalizers.blankToNull(googleName), 100));
            }
            return member;
        });
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
