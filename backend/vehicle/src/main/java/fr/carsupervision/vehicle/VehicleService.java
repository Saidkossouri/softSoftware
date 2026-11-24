package fr.carsupervision.vehicle;

import fr.carsupervision.api.model.FieldError;
import fr.carsupervision.api.model.VehicleInput;
import fr.carsupervision.common.Normalizers;
import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.security.CurrentMember;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestion du parc de véhicules (FR-014 à FR-018). */
@Service
public class VehicleService {

    static final String REGISTRATION_ALREADY_EXISTS_MESSAGE =
            "Cette immatriculation est déjà utilisée par un autre véhicule (actif ou archivé).";
    static final String FORBIDDEN_MESSAGE = "Vous n'avez pas accès à ce véhicule.";

    private static final int TEXT_MAX_LENGTH = 50;
    private static final int MAX_MILEAGE_KM = 2_000_000;
    private static final Pattern REGISTRATION_KEY = Pattern.compile("^[A-Z0-9]{2,20}$");
    private static final Pattern VIN = Pattern.compile("^[A-HJ-NPR-Z0-9]{17}$");

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;
    private final CurrentMember currentMember;
    private final Clock clock;

    public VehicleService(VehicleRepository vehicleRepository, VehicleMapper vehicleMapper,
            CurrentMember currentMember, Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
        this.currentMember = currentMember;
        this.clock = clock;
    }

    /**
     * Véhicules du statut demandé, triés par surnom. Les attributions d'un USER arrivent avec
     * l'histoire 4 : d'ici là, un USER reçoit une liste vide et ne peut pas voir les archivés.
     */
    @Transactional(readOnly = true)
    public List<Vehicle> list(VehicleStatus status) {
        VehicleStatus requested = status == null ? VehicleStatus.ACTIVE : status;
        if (!currentMember.get().isAdmin()) {
            if (requested == VehicleStatus.ARCHIVED) {
                throw forbidden();
            }
            return List.of();
        }
        return vehicleRepository.findAllByStatusOrderByNicknameAsc(requested);
    }

    /** ADMIN : 404 si inconnu. USER : 403 sans détail pour l'instant (attributions en histoire 4). */
    @Transactional(readOnly = true)
    public Vehicle get(UUID id) {
        if (!currentMember.get().isAdmin()) {
            throw forbidden();
        }
        return find(id);
    }

    @Transactional
    public Vehicle create(VehicleInput input) {
        Vehicle vehicle = new Vehicle(OffsetDateTime.now(clock));
        apply(input, vehicle);
        if (vehicleRepository.existsByRegistrationKey(vehicle.getRegistrationKey())) {
            throw registrationAlreadyExists();
        }
        return vehicleRepository.saveAndFlush(vehicle);
    }

    /** Remplace toutes les informations du véhicule ; le statut est inchangé. */
    @Transactional
    public Vehicle update(UUID id, VehicleInput input) {
        Vehicle vehicle = find(id);
        apply(input, vehicle);
        if (vehicleRepository.existsByRegistrationKeyAndIdNot(vehicle.getRegistrationKey(), id)) {
            throw registrationAlreadyExists();
        }
        vehicle.setUpdatedAt(OffsetDateTime.now(clock));
        return vehicleRepository.saveAndFlush(vehicle);
    }

    /** Idempotent : archiver un véhicule déjà archivé le renvoie tel quel. */
    @Transactional
    public Vehicle archive(UUID id) {
        return changeStatus(id, VehicleStatus.ARCHIVED);
    }

    /** Idempotent : réactiver un véhicule actif le renvoie tel quel. */
    @Transactional
    public Vehicle reactivate(UUID id) {
        return changeStatus(id, VehicleStatus.ACTIVE);
    }

    private Vehicle changeStatus(UUID id, VehicleStatus status) {
        Vehicle vehicle = find(id);
        if (vehicle.getStatus() != status) {
            vehicle.setStatus(status);
            vehicle.setUpdatedAt(OffsetDateTime.now(clock));
        }
        return vehicle;
    }

    private Vehicle find(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Ce véhicule n'existe pas."));
    }

    /** Valide et normalise la saisie, puis l'applique au véhicule ; toutes les erreurs d'un coup. */
    private void apply(VehicleInput input, Vehicle vehicle) {
        List<FieldError> errors = new ArrayList<>();
        String nickname = requiredText("nickname", input.getNickname(), errors);
        String make = requiredText("make", input.getMake(), errors);
        String model = requiredText("model", input.getModel(), errors);

        String registration = Normalizers.normalizeRegistrationDisplay(input.getRegistration());
        String registrationKey = Normalizers.registrationKey(registration);
        if (registrationKey == null || !REGISTRATION_KEY.matcher(registrationKey).matches()) {
            errors.add(new FieldError("registration",
                    "Saisissez une immatriculation de 2 à 20 lettres ou chiffres (espaces et tirets acceptés)."));
        }

        Integer year = input.getFirstRegistrationYear();
        int currentYear = Year.now(clock).getValue();
        if (year != null && (year < 1900 || year > currentYear)) {
            errors.add(new FieldError("firstRegistrationYear",
                    "L'année doit être comprise entre 1900 et " + currentYear + "."));
        }

        Integer mileage = input.getInitialMileageKm();
        if (mileage == null || mileage < 0 || mileage > MAX_MILEAGE_KM) {
            errors.add(new FieldError("initialMileageKm",
                    "Le kilométrage doit être compris entre 0 et 2 000 000 km."));
        }

        String vin = Normalizers.blankToNull(input.getVin());
        if (vin != null) {
            vin = vin.toUpperCase(Locale.ROOT);
            if (!VIN.matcher(vin).matches()) {
                errors.add(new FieldError("vin",
                        "Le VIN compte 17 lettres ou chiffres, sans les lettres I, O et Q."));
            }
        }

        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                    "Certains champs sont invalides. Corrigez-les puis réessayez.", errors);
        }

        vehicle.setNickname(nickname);
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setRegistration(registration, registrationKey);
        vehicle.setFirstRegistrationYear(year);
        vehicle.setInitialMileageKm(mileage);
        vehicle.setVin(vin);
        vehicle.setEnergyType(input.getEnergyType() == null ? null : vehicleMapper.toEntity(input.getEnergyType()));
    }

    private static String requiredText(String field, String value, List<FieldError> errors) {
        String text = Normalizers.blankToNull(value);
        if (text == null) {
            errors.add(new FieldError(field, "Ce champ est obligatoire."));
        } else if (text.length() > TEXT_MAX_LENGTH) {
            errors.add(new FieldError(field, "50 caractères au maximum."));
        }
        return text;
    }

    private static ApiException registrationAlreadyExists() {
        return ApiException.conflict(ErrorCode.REGISTRATION_ALREADY_EXISTS, REGISTRATION_ALREADY_EXISTS_MESSAGE);
    }

    private static ApiException forbidden() {
        return new ApiException(HttpStatus.FORBIDDEN, ErrorCode.VEHICLE_FORBIDDEN, FORBIDDEN_MESSAGE);
    }
}
