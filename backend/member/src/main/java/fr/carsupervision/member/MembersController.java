package fr.carsupervision.member;

import fr.carsupervision.api.MembersApi;
import fr.carsupervision.api.model.MemberCreateRequest;
import fr.carsupervision.api.model.MemberUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Administration des membres : réservée aux ADMIN, contrôlée côté serveur (FR-013). */
@RestController
public class MembersController implements MembersApi {

    private final MemberService memberService;
    private final MemberMapper memberMapper;

    public MembersController(MemberService memberService, MemberMapper memberMapper) {
        this.memberService = memberService;
        this.memberMapper = memberMapper;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<fr.carsupervision.api.model.Member>> listMembers() {
        return ResponseEntity.ok(memberService.list().stream().map(memberMapper::toDto).toList());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.Member> createMember(MemberCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberMapper.toDto(memberService.create(request)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.Member> getMember(UUID memberId) {
        return ResponseEntity.ok(memberMapper.toDto(memberService.get(memberId)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<fr.carsupervision.api.model.Member> updateMember(UUID memberId,
            MemberUpdateRequest request) {
        return ResponseEntity.ok(memberMapper.toDto(memberService.update(memberId, request)));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMember(UUID memberId) {
        memberService.delete(memberId);
        return ResponseEntity.noContent().build();
    }
}
