package fr.carsupervision.administration.member;

import fr.carsupervision.api.model.CurrentMember;
import fr.carsupervision.security.AuthenticatedMember;
import org.mapstruct.Mapper;

/** Conversion entité / membre connecté vers les DTOs générés depuis le contrat. */
@Mapper
public interface MemberMapper {

    CurrentMember toCurrentMember(AuthenticatedMember member);

    fr.carsupervision.api.model.Member toDto(Member member);

    fr.carsupervision.api.model.Role toDto(Role role);
}
