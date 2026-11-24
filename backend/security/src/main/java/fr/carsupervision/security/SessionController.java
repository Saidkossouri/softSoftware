package fr.carsupervision.security;

import fr.carsupervision.api.SessionApi;
import fr.carsupervision.api.model.CurrentMember;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController implements SessionApi {

    private final fr.carsupervision.security.CurrentMember currentMember;
    private final CurrentMemberMapper currentMemberMapper;

    public SessionController(fr.carsupervision.security.CurrentMember currentMember, CurrentMemberMapper currentMemberMapper) {
        this.currentMember = currentMember;
        this.currentMemberMapper = currentMemberMapper;
    }

    @Override
    public ResponseEntity<CurrentMember> getCurrentMember() {
        return ResponseEntity.ok(currentMemberMapper.toDto(currentMember.get()));
    }
}
