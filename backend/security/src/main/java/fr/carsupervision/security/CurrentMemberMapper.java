package fr.carsupervision.security;

import fr.carsupervision.api.model.CurrentMember;
import org.mapstruct.Mapper;

/** Conversion du membre connecté vers le DTO généré depuis le contrat. */
@Mapper
public interface CurrentMemberMapper {

    CurrentMember toDto(AuthenticatedMember member);
}
