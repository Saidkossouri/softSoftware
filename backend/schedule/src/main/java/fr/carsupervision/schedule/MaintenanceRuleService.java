package fr.carsupervision.schedule;

import fr.carsupervision.api.model.MaintenanceRuleInput;
import fr.carsupervision.api.model.MaintenanceRuleUpdate;
import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.common.time.Today;
import fr.carsupervision.maintenance.MaintenanceType;
import fr.carsupervision.maintenance.MaintenanceTypeCategory;
import fr.carsupervision.maintenance.MaintenanceTypeRepository;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleAccessGuard;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Règles d'échéance (FR-021, FR-022) : tout membre ayant accès au véhicule les crée, modifie et
 * supprime (pas de contrôle d'auteur). Une seule règle par opération et par véhicule.
 */
@Service
public class MaintenanceRuleService {

    static final String NOT_FOUND_MESSAGE = "Cette règle d'échéance n'existe pas.";

    private final MaintenanceRuleRepository ruleRepository;
    private final MaintenanceTypeRepository typeRepository;
    private final VehicleAccessGuard accessGuard;
    private final Today today;
    private final Clock clock;

    public MaintenanceRuleService(MaintenanceRuleRepository ruleRepository, MaintenanceTypeRepository typeRepository,
            VehicleAccessGuard accessGuard, Today today, Clock clock) {
        this.ruleRepository = ruleRepository;
        this.typeRepository = typeRepository;
        this.accessGuard = accessGuard;
        this.today = today;
        this.clock = clock;
    }

    /** Point de départ par défaut : aujourd'hui et le kilométrage actuel du véhicule. */
    @Transactional
    public MaintenanceRule create(UUID vehicleId, MaintenanceRuleInput input) {
        Vehicle vehicle = accessGuard.lockForWrite(vehicleId);
        MaintenanceType type = typeRepository.findById(input.getTypeId())
                .filter(found -> found.getCategory() == MaintenanceTypeCategory.MAINTENANCE)
                .orElseThrow(() -> ApiException.invalidField("typeId", "Ce type d'entretien n'existe pas."));
        if (!type.isActive()) {
            throw ApiException.invalidField("typeId",
                    "Le type « " + type.getName() + " » n'est plus proposé. Choisissez-en un autre.");
        }
        if (ruleRepository.existsByVehicleIdAndTypeId(vehicleId, type.getId())) {
            throw ApiException.conflict(ErrorCode.RULE_ALREADY_EXISTS, "Une règle « " + type.getName()
                    + " » existe déjà pour ce véhicule : modifiez-la plutôt que d'en créer une autre.");
        }
        LocalDate baselineDate = input.getBaselineDate() != null ? input.getBaselineDate() : today.date();
        int baselineKm = input.getBaselineMileageKm() != null
                ? input.getBaselineMileageKm()
                : vehicle.getCurrentMileageKm();
        MaintenanceRule rule = new MaintenanceRule(vehicleId, type, OffsetDateTime.now(clock));
        apply(rule, input.getIntervalKm(), input.getIntervalMonths(), baselineDate, baselineKm);
        return ruleRepository.saveAndFlush(rule);
    }

    /** Le type d'une règle ne change pas. */
    @Transactional
    public MaintenanceRule update(UUID ruleId, MaintenanceRuleUpdate input) {
        MaintenanceRule rule = find(ruleId);
        accessGuard.lockForWrite(rule.getVehicleId());
        apply(rule, input.getIntervalKm(), input.getIntervalMonths(), input.getBaselineDate(),
                input.getBaselineMileageKm());
        return ruleRepository.saveAndFlush(rule);
    }

    @Transactional
    public void delete(UUID ruleId) {
        MaintenanceRule rule = find(ruleId);
        accessGuard.lockForWrite(rule.getVehicleId());
        ruleRepository.delete(rule);
    }

    private MaintenanceRule find(UUID ruleId) {
        return ruleRepository.findById(ruleId).orElseThrow(() -> accessGuard.missing(NOT_FOUND_MESSAGE));
    }

    private void apply(MaintenanceRule rule, Integer intervalKm, Integer intervalMonths, LocalDate baselineDate,
            int baselineKm) {
        if (intervalKm == null && intervalMonths == null) {
            throw ApiException.invalidField("intervalKm",
                    "Indiquez une fréquence en kilomètres, en mois, ou les deux.");
        }
        if (baselineDate.isAfter(today.date())) {
            throw ApiException.invalidField("baselineDate",
                    "La dernière réalisation ne peut pas être dans le futur.");
        }
        rule.update(intervalKm, intervalMonths, baselineDate, baselineKm, OffsetDateTime.now(clock));
    }
}
