package fr.carsupervision.security;

import fr.carsupervision.administration.member.MemberMapper;
import fr.carsupervision.api.SessionApi;
import fr.carsupervision.api.model.CurrentMember;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController implements SessionApi {

    private final fr.carsupervision.security.CurrentMember currentMember;
    private final MemberMapper memberMapper;

    public SessionController(fr.carsupervision.security.CurrentMember currentMember, MemberMapper memberMapper) {
        this.currentMember = currentMember;
        this.memberMapper = memberMapper;
    }

    @Override
    public ResponseEntity<CurrentMember> getCurrentMember() {
        return ResponseEntity.ok(memberMapper.toCurrentMember(currentMember.get()));
    }
}
