package fr.carsupervision.maintenance;

import fr.carsupervision.vehicle.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Relevés vers les DTOs du contrat ; {@code canEdit} et {@code currentMileage} viennent du service. */
@Mapper
public interface MileageReadingMapper {

    @Mapping(target = "canEdit", source = "canEdit")
    @Mapping(target = "currentMileage", expression = "java(currentMileage(vehicle))")
    @Mapping(target = "id", source = "reading.id")
    @Mapping(target = "createdAt", source = "reading.createdAt")
    @Mapping(target = "updatedAt", source = "reading.updatedAt")
    fr.carsupervision.api.model.MileageReading toDto(MileageReading reading, Vehicle vehicle, boolean canEdit);

    default fr.carsupervision.api.model.CurrentMileage currentMileage(Vehicle vehicle) {
        return new fr.carsupervision.api.model.CurrentMileage(
                vehicle.getCurrentMileageKm(), vehicle.getCurrentMileageDate());
    }
}
