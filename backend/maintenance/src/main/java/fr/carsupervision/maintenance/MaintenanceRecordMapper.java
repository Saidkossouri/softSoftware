package fr.carsupervision.maintenance;

import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entretiens vers les DTOs du contrat ; types triés dans l'ordre du catalogue. */
@Mapper(uses = MaintenanceTypeMapper.class)
public interface MaintenanceRecordMapper {

    @Mapping(target = "id", source = "record.id")
    @Mapping(target = "vehicleId", source = "record.vehicleId")
    @Mapping(target = "recordDate", source = "record.recordDate")
    @Mapping(target = "mileageKm", source = "record.mileageKm")
    @Mapping(target = "description", source = "record.description")
    @Mapping(target = "costCents", source = "record.costCents")
    @Mapping(target = "garage", source = "record.garage")
    @Mapping(target = "notes", source = "record.notes")
    @Mapping(target = "authorName", source = "record.authorName")
    @Mapping(target = "createdAt", source = "record.createdAt")
    @Mapping(target = "updatedAt", source = "record.updatedAt")
    @Mapping(target = "types", expression = "java(typeRefs(view.record()))")
    @Mapping(target = "currentMileage", expression = "java(currentMileage(view.vehicle()))")
    fr.carsupervision.api.model.MaintenanceRecord toDto(MaintenanceRecordService.View view);

    fr.carsupervision.api.model.TypeRef toRef(MaintenanceType type);

    default List<fr.carsupervision.api.model.TypeRef> typeRefs(MaintenanceRecord record) {
        return record.getTypes().stream()
                .sorted(Comparator.comparingInt(MaintenanceType::getDisplayOrder).thenComparing(MaintenanceType::getName))
                .map(this::toRef)
                .toList();
    }

    default fr.carsupervision.api.model.CurrentMileage currentMileage(fr.carsupervision.vehicle.Vehicle vehicle) {
        return new fr.carsupervision.api.model.CurrentMileage(
                vehicle.getCurrentMileageKm(), vehicle.getCurrentMileageDate());
    }
}
