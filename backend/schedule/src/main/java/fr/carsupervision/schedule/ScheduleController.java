package fr.carsupervision.schedule;

import fr.carsupervision.api.ScheduleApi;
import fr.carsupervision.api.model.Deadline;
import fr.carsupervision.api.model.DeadlineCompletion;
import fr.carsupervision.api.model.DeadlineInput;
import fr.carsupervision.api.model.DeadlineSuggestion;
import fr.carsupervision.api.model.DueItem;
import fr.carsupervision.api.model.MaintenanceRule;
import fr.carsupervision.api.model.MaintenanceRuleInput;
import fr.carsupervision.api.model.MaintenanceRuleUpdate;
import fr.carsupervision.api.model.Reminders;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Opérations du tag {@code schedule} : le contrat les regroupe dans une seule interface
 * ({@link ScheduleApi}), d'où un seul contrôleur ; règles métier et droits dans les services.
 */
@RestController
public class ScheduleController implements ScheduleApi {

    private final MaintenanceRuleService ruleService;
    private final DeadlineService deadlineService;
    private final DueItemsService dueItemsService;
    private final RemindersService remindersService;
    private final DeadlineSuggestionService suggestionService;
    private final ScheduleMapper mapper;

    public ScheduleController(MaintenanceRuleService ruleService, DeadlineService deadlineService,
            DueItemsService dueItemsService, RemindersService remindersService,
            DeadlineSuggestionService suggestionService, ScheduleMapper mapper) {
        this.ruleService = ruleService;
        this.deadlineService = deadlineService;
        this.dueItemsService = dueItemsService;
        this.remindersService = remindersService;
        this.suggestionService = suggestionService;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<List<DueItem>> listVehicleDueItems(UUID vehicleId) {
        return ResponseEntity.ok(dueItemsService.forVehicle(vehicleId).stream().map(mapper::toDto).toList());
    }

    @Override
    public ResponseEntity<Reminders> getReminders() {
        return ResponseEntity.ok(mapper.toDto(remindersService.get()));
    }

    @Override
    public ResponseEntity<MaintenanceRule> createMaintenanceRule(UUID vehicleId, MaintenanceRuleInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(ruleService.create(vehicleId, input)));
    }

    @Override
    public ResponseEntity<MaintenanceRule> updateMaintenanceRule(UUID ruleId, MaintenanceRuleUpdate input) {
        return ResponseEntity.ok(mapper.toDto(ruleService.update(ruleId, input)));
    }

    @Override
    public ResponseEntity<Void> deleteMaintenanceRule(UUID ruleId) {
        ruleService.delete(ruleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Deadline> createDeadline(UUID vehicleId, DeadlineInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(deadlineService.create(vehicleId, input)));
    }

    @Override
    public ResponseEntity<Deadline> updateDeadline(UUID deadlineId, DeadlineInput input) {
        return ResponseEntity.ok(mapper.toDto(deadlineService.update(deadlineId, input)));
    }

    @Override
    public ResponseEntity<Void> deleteDeadline(UUID deadlineId) {
        deadlineService.delete(deadlineId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DeadlineCompletion> completeDeadline(UUID deadlineId) {
        return ResponseEntity.ok(mapper.toDto(deadlineService.complete(deadlineId)));
    }

    @Override
    public ResponseEntity<List<DeadlineSuggestion>> listDeadlineSuggestions(UUID vehicleId) {
        return ResponseEntity.ok(suggestionService.list(vehicleId).stream().map(mapper::toSuggestion).toList());
    }

    @Override
    public ResponseEntity<Void> dismissDeadlineSuggestion(UUID vehicleId, UUID typeId) {
        suggestionService.dismiss(vehicleId, typeId);
        return ResponseEntity.noContent().build();
    }
}
