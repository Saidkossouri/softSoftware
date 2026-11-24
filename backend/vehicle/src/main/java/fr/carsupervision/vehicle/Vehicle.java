package fr.carsupervision.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Un véhicule du parc familial. */
@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    private UUID id;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 50)
    private String model;

    /** Immatriculation telle qu'affichée (majuscules). */
    @Column(nullable = false, length = 20)
    private String registration;

    /** Clé d'unicité : majuscules, sans espaces ni tirets. */
    @Column(name = "registration_key", nullable = false, unique = true, length = 20)
    private String registrationKey;

    @Column(name = "first_registration_year")
    private Short firstRegistrationYear;

    @Column(name = "initial_mileage_km", nullable = false)
    private Integer initialMileageKm;

    @Column(length = 17)
    private String vin;

    @Enumerated(EnumType.STRING)
    @Column(name = "energy_type", length = 12)
    private EnergyType energyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VehicleStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Vehicle() {
        // pour JPA
    }

    /** Nouveau véhicule actif ; les informations sont renseignées ensuite par le service. */
    public Vehicle(OffsetDateTime createdAt) {
        this.id = UUID.randomUUID();
        this.status = VehicleStatus.ACTIVE;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRegistration() {
        return registration;
    }

    public String getRegistrationKey() {
        return registrationKey;
    }

    public void setRegistration(String registration, String registrationKey) {
        this.registration = registration;
        this.registrationKey = registrationKey;
    }

    public Integer getFirstRegistrationYear() {
        return firstRegistrationYear == null ? null : firstRegistrationYear.intValue();
    }

    public void setFirstRegistrationYear(Integer firstRegistrationYear) {
        this.firstRegistrationYear = firstRegistrationYear == null ? null : firstRegistrationYear.shortValue();
    }

    public Integer getInitialMileageKm() {
        return initialMileageKm;
    }

    public void setInitialMileageKm(Integer initialMileageKm) {
        this.initialMileageKm = initialMileageKm;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public EnergyType getEnergyType() {
        return energyType;
    }

    public void setEnergyType(EnergyType energyType) {
        this.energyType = energyType;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == VehicleStatus.ACTIVE;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
