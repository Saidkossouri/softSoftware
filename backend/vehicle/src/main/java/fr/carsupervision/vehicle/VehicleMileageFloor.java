package fr.carsupervision.vehicle;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * Port implémenté par le module maintenance (le module vehicle ne dépend pas de lui) : plus petit
 * kilométrage déjà saisi sur un véhicule, relevés et entretiens confondus (FR-009).
 */
public interface VehicleMileageFloor {

    /** Vide si le véhicule n'a aucune saisie. */
    OptionalInt lowestRecordedMileage(UUID vehicleId);
}
