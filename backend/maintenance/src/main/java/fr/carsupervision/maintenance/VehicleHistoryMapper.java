package fr.carsupervision.maintenance;

import fr.carsupervision.api.model.CostTotals;
import fr.carsupervision.api.model.CurrentMileage;
import fr.carsupervision.api.model.HistoryEntry;
import fr.carsupervision.api.model.HistoryEntryKind;
import fr.carsupervision.api.model.VehicleHistory;
import org.springframework.stereotype.Component;

/** Historique vers le DTO du contrat ; les types d'un entretien suivent l'ordre du catalogue. */
@Component
public class VehicleHistoryMapper {

    private final MaintenanceRecordMapper recordMapper;

    public VehicleHistoryMapper(MaintenanceRecordMapper recordMapper) {
        this.recordMapper = recordMapper;
    }

    public VehicleHistory toDto(VehicleHistoryService.History history) {
        var vehicle = history.vehicle();
        var totals = history.totals();
        return new VehicleHistory(vehicle.getId(),
                new CurrentMileage(vehicle.getCurrentMileageKm(), vehicle.getCurrentMileageDate()),
                new CostTotals(totals.year(), Math.toIntExact(totals.currentYearCents()),
                        Math.toIntExact(totals.allTimeCents())),
                history.entries().stream().map(this::toEntry).toList());
    }

    private HistoryEntry toEntry(VehicleHistoryService.Entry entry) {
        if (entry.reading() != null) {
            MileageReading reading = entry.reading();
            return new HistoryEntry(HistoryEntryKind.READING, reading.getId(), reading.getReadingDate(),
                    reading.getMileageKm(), reading.getAuthorName(), entry.canEdit())
                    .note(reading.getNote());
        }
        MaintenanceRecord record = entry.record();
        return new HistoryEntry(HistoryEntryKind.RECORD, record.getId(), record.getRecordDate(),
                record.getMileageKm(), record.getAuthorName(), entry.canEdit())
                .types(recordMapper.typeRefs(record))
                .description(record.getDescription())
                .costCents(record.getCostCents())
                .garage(record.getGarage())
                .notes(record.getNotes())
                .documentCount(entry.documentCount());
    }
}
