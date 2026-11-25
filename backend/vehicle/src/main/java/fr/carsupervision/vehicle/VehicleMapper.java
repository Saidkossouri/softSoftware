package fr.carsupervision.vehicle;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Conversion des véhicules vers les DTOs générés depuis le contrat (jamais d'entité exposée). */
@Mapper
public interface VehicleMapper {

    fr.carsupervision.api.model.Vehicle toDto(Vehicle vehicle);

    fr.carsupervision.api.model.VehicleStatus toDto(VehicleStatus status);

    VehicleStatus toEntity(fr.carsupervision.api.model.VehicleStatus status);

    EnergyType toEntity(fr.carsupervision.api.model.EnergyType energyType);

    /** Attributions d'un membre : uniquement les identifiants des véhicules. */
    default fr.carsupervision.api.model.VehicleAccess toAccessDto(Collection<UUID> vehicleIds) {
        fr.carsupervision.api.model.VehicleAccess dto = new fr.carsupervision.api.model.VehicleAccess();
        dto.setVehicleIds(new LinkedHashSet<>(vehicleIds));
        return dto;
    }
}
