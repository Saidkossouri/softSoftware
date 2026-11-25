package fr.carsupervision.schedule;

import fr.carsupervision.api.model.CurrentMileage;
import fr.carsupervision.api.model.DeadlineCompletion;
import fr.carsupervision.api.model.DeadlineSuggestion;
import fr.carsupervision.api.model.DueItem;
import fr.carsupervision.api.model.DueItemKind;
import fr.carsupervision.api.model.DueStatus;
import fr.carsupervision.api.model.MileageReadingReminder;
import fr.carsupervision.api.model.Reminders;
import fr.carsupervision.api.model.VehicleDueItem;
import fr.carsupervision.maintenance.MaintenanceRecordMapper;
import fr.carsupervision.maintenance.MaintenanceType;
import org.springframework.stereotype.Component;

/** Règles, échéances, échéances calculées et rappels vers les DTOs du contrat. */
@Component
public class ScheduleMapper {

    private final MaintenanceRecordMapper recordMapper;

    public ScheduleMapper(MaintenanceRecordMapper recordMapper) {
        this.recordMapper = recordMapper;
    }

    public fr.carsupervision.api.model.MaintenanceRule toDto(MaintenanceRule rule) {
        return new fr.carsupervision.api.model.MaintenanceRule(rule.getId(), rule.getVehicleId(),
                recordMapper.toRef(rule.getType()), rule.getBaselineDate(), rule.getBaselineMileageKm())
                .intervalKm(rule.getIntervalKm())
                .intervalMonths(rule.getIntervalMonths());
    }

    public fr.carsupervision.api.model.Deadline toDto(Deadline deadline) {
        return new fr.carsupervision.api.model.Deadline(deadline.getId(), deadline.getVehicleId(),
                deadline.getLabel(), deadline.getDueDate(), deadline.isDone())
                .type(deadline.getType() == null ? null : recordMapper.toRef(deadline.getType()))
                .recurrenceMonths(deadline.getRecurrenceMonths())
                .doneAt(deadline.getDoneAt());
    }

    public DeadlineCompletion toDto(DeadlineService.Completion completion) {
        return new DeadlineCompletion(toDto(completion.completed()))
                .next(completion.next() == null ? null : toDto(completion.next()));
    }

    public DueItem toDto(DueItemsService.Item item) {
        var due = item.due();
        DueItem dto = new DueItem(item.rule() != null ? DueItemKind.RULE : DueItemKind.DEADLINE,
                DueStatus.valueOf(due.state().name()))
                .dueDate(due.dueDate())
                .dueMileageKm(due.dueMileageKm())
                .remainingDays(due.remainingDays())
                .remainingKm(due.remainingKm());
        if (item.rule() != null) {
            MaintenanceRule rule = item.rule();
            return dto.ruleId(rule.getId())
                    .type(recordMapper.toRef(rule.getType()))
                    .label(rule.getType().getName())
                    .intervalKm(rule.getIntervalKm())
                    .intervalMonths(rule.getIntervalMonths())
                    .lastDoneDate(item.lastDate())
                    .lastDoneMileageKm(item.lastKm())
                    .lastDoneRecordId(item.lastRecordId());
        }
        Deadline deadline = item.deadline();
        return dto.deadlineId(deadline.getId())
                .type(deadline.getType() == null ? null : recordMapper.toRef(deadline.getType()))
                .label(deadline.getLabel())
                .recurrenceMonths(deadline.getRecurrenceMonths());
    }

    public Reminders toDto(RemindersService.Reminders reminders) {
        return new Reminders(
                reminders.dueItems().stream()
                        .map(entry -> new VehicleDueItem(entry.vehicle().getId(), entry.vehicle().getNickname(),
                                toDto(entry.item())))
                        .toList(),
                reminders.mileageReadings().stream()
                        .map(vehicle -> new MileageReadingReminder(vehicle.getId(), vehicle.getNickname(),
                                new CurrentMileage(vehicle.getCurrentMileageKm(), vehicle.getCurrentMileageDate())))
                        .toList());
    }

    public DeadlineSuggestion toSuggestion(MaintenanceType type) {
        return new DeadlineSuggestion(recordMapper.toRef(type)).recurrenceMonths(type.getDefaultIntervalMonths());
    }
}
