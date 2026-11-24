package fr.carsupervision.vehicle;

import org.mapstruct.Mapper;

/** Conversion des véhicules vers les DTOs générés depuis le contrat (jamais d'entité exposée). */
@Mapper
public interface VehicleMapper {

    fr.carsupervision.api.model.Vehicle toDto(Vehicle vehicle);

    fr.carsupervision.api.model.VehicleStatus toDto(VehicleStatus status);

    VehicleStatus toEntity(fr.carsupervision.api.model.VehicleStatus status);

    EnergyType toEntity(fr.carsupervision.api.model.EnergyType energyType);
}
