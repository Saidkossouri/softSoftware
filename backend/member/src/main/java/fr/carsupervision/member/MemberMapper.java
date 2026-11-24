package fr.carsupervision.member;

import org.mapstruct.Mapper;

/** Conversion des entités du module membre vers les DTOs générés depuis le contrat. */
@Mapper
public interface MemberMapper {

    fr.carsupervision.api.model.Member toDto(Member member);

    fr.carsupervision.api.model.Role toDto(Role role);
}
