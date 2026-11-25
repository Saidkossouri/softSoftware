package fr.carsupervision.support;

import fr.carsupervision.member.Member;
import fr.carsupervision.vehicle.Vehicle;
import fr.carsupervision.vehicle.VehicleAccess;
import fr.carsupervision.vehicle.VehicleAccessRepository;
import fr.carsupervision.vehicle.VehicleRepository;
import fr.carsupervision.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.TestComponent;

/** Fabriques de véhicules en base et corps JSON des saisies de la fonctionnalité 002. */
@TestComponent
public class TestVehicles {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private final VehicleRepository vehicleRepository;
    private final VehicleAccessRepository accessRepository;
    private final MutableClock clock;

    public TestVehicles(VehicleRepository vehicleRepository, VehicleAccessRepository accessRepository,
            MutableClock clock) {
        this.vehicleRepository = vehicleRepository;
        this.accessRepository = accessRepository;
        this.clock = clock;
    }

    /** Véhicule actif ; kilométrage actuel = kilométrage de départ, daté du jour de l'horloge de test. */
    public Vehicle active(String nickname, int initialKm) {
        return save(nickname, initialKm, VehicleStatus.ACTIVE, null);
    }

    /** Véhicule actif mis en circulation l'année donnée (suggestions selon l'âge). */
    public Vehicle active(String nickname, int initialKm, int firstRegistrationYear) {
        return save(nickname, initialKm, VehicleStatus.ACTIVE, firstRegistrationYear);
    }

    public Vehicle archived(String nickname, int initialKm) {
        return save(nickname, initialKm, VehicleStatus.ARCHIVED, null);
    }

    public void assign(Member member, Vehicle vehicle) {
        accessRepository.save(new VehicleAccess(member.getId(), vehicle.getId(), now()));
    }

    public Vehicle reload(Vehicle vehicle) {
        return vehicleRepository.findById(vehicle.getId()).orElseThrow();
    }

    /** Corps JSON d'un relevé kilométrique ({@code MileageReadingInput}). */
    public static String reading(LocalDate date, int km) {
        return """
                {"readingDate":"%s","mileageKm":%d}""".formatted(date, km);
    }

    public static String reading(LocalDate date, int km, boolean confirmLargeJump) {
        return """
                {"readingDate":"%s","mileageKm":%d,"confirmLargeJump":%s}""".formatted(date, km, confirmLargeJump);
    }

    private Vehicle save(String nickname, int initialKm, VehicleStatus status, Integer year) {
        OffsetDateTime now = now();
        Vehicle vehicle = new Vehicle(now);
        int n = SEQUENCE.incrementAndGet();
        vehicle.setNickname(nickname);
        vehicle.setMake("Dacia");
        vehicle.setModel("Logan");
        vehicle.setRegistration("12345-A-" + n, "12345A" + n);
        vehicle.setInitialMileageKm(initialKm);
        vehicle.setFirstRegistrationYear(year);
        vehicle.setStatus(status);
        vehicle.setCurrentMileage(initialKm, now.toLocalDate());
        return vehicleRepository.saveAndFlush(vehicle);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.ofHours(1));
    }
}
