package fr.carsupervision.maintenance;

import org.mapstruct.Mapper;

/** Types du catalogue vers les DTOs du contrat ({@code MaintenanceType}, {@code TypeRef}). */
@Mapper
public interface MaintenanceTypeMapper {

    fr.carsupervision.api.model.MaintenanceType toDto(MaintenanceType type);

    fr.carsupervision.api.model.TypeRef toRef(MaintenanceType type);

    fr.carsupervision.api.model.MaintenanceTypeCategory toDto(MaintenanceTypeCategory category);

    MaintenanceTypeCategory toEntity(fr.carsupervision.api.model.MaintenanceTypeCategory category);
}
