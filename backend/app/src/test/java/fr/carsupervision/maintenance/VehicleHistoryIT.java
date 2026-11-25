package fr.carsupervision.maintenance;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestCatalogue.CLIMATISATION;
import static fr.carsupervision.support.TestCatalogue.FILTRE_A_HUILE;
import static fr.carsupervision.support.TestCatalogue.VIDANGE;
import static fr.carsupervision.support.TestCatalogue.record;
import static fr.carsupervision.support.TestVehicles.reading;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import fr.carsupervision.vehicle.Vehicle;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** US3 : historique et dépenses d'un véhicule (FR-014 à FR-016, FR-018, SC-006). Horloge au 15/06/2026. */
class VehicleHistoryIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private Member user;
    private Vehicle vehicle;

    @BeforeEach
    void vehicleAssignedToUser() {
        user = members.create(USER, Role.USER);
        vehicle = vehicles.active("A", 40_000);
        vehicles.assign(user, vehicle);
    }

    private String history() {
        return "/api/vehicles/" + vehicle.getId() + "/history";
    }

    private void addRecord(String email, String body) throws Exception {
        mockMvc.perform(as(email, json(post("/api/vehicles/" + vehicle.getId() + "/maintenance-records"), body)))
                .andExpect(status().isCreated());
    }

    private void addReading(LocalDate date, int km) throws Exception {
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + vehicle.getId() + "/mileage-readings"),
                reading(date, km)))).andExpect(status().isCreated());
    }

    /** 1 200 DH l'an dernier, 300 DH et 800 DH cette année, un entretien sans coût, deux relevés. */
    private void sampleHistory() throws Exception {
        addRecord(INITIAL_ADMIN, record("2025-11-10", 45_000, "\"costCents\":120000", VIDANGE));
        addRecord(USER, record("2026-02-01", 47_000, "\"costCents\":30000", FILTRE_A_HUILE));
        addReading(LocalDate.parse("2026-03-01"), 48_000);
        addRecord(USER, record("2026-04-01", 48_500, null, CLIMATISATION));
        addRecord(USER, record("2026-05-20", 49_000, "\"costCents\":80000,\"garage\":\"Garage Atlas\"",
                FILTRE_A_HUILE, VIDANGE));
        addReading(LocalDate.parse("2026-05-20"), 49_500);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return as(USER, request);
    }

    @Test
    void listsEntriesNewestFirstWithTotals() throws Exception {
        sampleHistory();

        mockMvc.perform(asUser(get(history())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(vehicle.getId().toString()))
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(49500))
                .andExpect(jsonPath("$.currentMileage.date").value("2026-05-20"))
                .andExpect(jsonPath("$.costTotals.year").value(2026))
                .andExpect(jsonPath("$.costTotals.currentYearCents").value(110000))
                .andExpect(jsonPath("$.costTotals.allTimeCents").value(230000))
                .andExpect(jsonPath("$.entries[*].kind")
                        .value(contains("READING", "RECORD", "RECORD", "READING", "RECORD", "RECORD")))
                .andExpect(jsonPath("$.entries[*].mileageKm")
                        .value(contains(49500, 49000, 48500, 48000, 47000, 45000)))
                .andExpect(jsonPath("$.entries[1].types[*].name").value(contains("Vidange", "Filtre à huile")))
                .andExpect(jsonPath("$.entries[1].garage").value("Garage Atlas"))
                .andExpect(jsonPath("$.entries[1].costCents").value(80000))
                .andExpect(jsonPath("$.entries[1].documentCount").value(0))
                .andExpect(jsonPath("$.entries[1].canEdit").value(true))
                .andExpect(jsonPath("$.entries[2].costCents").doesNotExist())
                .andExpect(jsonPath("$.entries[5].authorName").value(INITIAL_ADMIN))
                .andExpect(jsonPath("$.entries[5].canEdit").value(false))
                .andExpect(conformsToContract());

        mockMvc.perform(as(INITIAL_ADMIN, get(history())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[*].canEdit").value(contains(true, true, true, true, true, true)));
    }

    @Test
    void filtersByTypeAndHidesReadings() throws Exception {
        sampleHistory();

        mockMvc.perform(asUser(get(history()).param("typeId", VIDANGE.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[*].date").value(contains("2026-05-20", "2025-11-10")))
                .andExpect(jsonPath("$.entries[*].kind").value(contains("RECORD", "RECORD")))
                .andExpect(jsonPath("$.costTotals.allTimeCents").value(230000))
                .andExpect(conformsToContract());

        mockMvc.perform(asUser(get(history()).param("recordsOnly", "true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[*].mileageKm").value(contains(49000, 48500, 47000, 45000)))
                .andExpect(conformsToContract());
    }

    @Test
    void renamedTypeShowsItsNewName() throws Exception {
        addRecord(USER, record("2026-05-20", 49_000, null, VIDANGE));
        jdbc.update("update maintenance_type set name = 'Vidange moteur' where id = ?", VIDANGE);

        mockMvc.perform(asUser(get(history())))
                .andExpect(jsonPath("$.entries[0].types[0].name").value("Vidange moteur"));
    }

    @Test
    void emptyVehicleShowsInitialMileage() throws Exception {
        mockMvc.perform(asUser(get(history())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(40000))
                .andExpect(jsonPath("$.costTotals.currentYearCents").value(0))
                .andExpect(jsonPath("$.costTotals.allTimeCents").value(0))
                .andExpect(jsonPath("$.entries", hasSize(0)))
                .andExpect(conformsToContract());
    }

    @Test
    void currentYearFollowsMoroccanCalendar() throws Exception {
        addRecord(USER, record("2026-05-20", 49_000, "\"costCents\":50000", VIDANGE));
        // 31/12/2026 à 23 h 30 UTC : encore le 31 décembre au Maroc (UTC+0), l'année reste 2026.
        clock.set(java.time.Instant.parse("2026-12-31T23:30:00Z"));

        mockMvc.perform(asUser(get(history())))
                .andExpect(jsonPath("$.costTotals.year").value(2026))
                .andExpect(jsonPath("$.costTotals.currentYearCents").value(50000));

        clock.set(java.time.Instant.parse("2027-01-01T00:30:00Z"));
        mockMvc.perform(asUser(get(history())))
                .andExpect(jsonPath("$.costTotals.year").value(2027))
                .andExpect(jsonPath("$.costTotals.currentYearCents").value(0))
                .andExpect(jsonPath("$.costTotals.allTimeCents").value(50000));
    }

    @Test
    void accessFollowsVehicleReadRights() throws Exception {
        members.create("autre@example.com", Role.USER);
        mockMvc.perform(as("autre@example.com", get(history())))
                .andExpect(status().isForbidden())
                .andExpect(conformsToContract());

        mockMvc.perform(as(INITIAL_ADMIN, get("/api/vehicles/" + UUID.randomUUID() + "/history")))
                .andExpect(status().isNotFound())
                .andExpect(conformsToContract());

        Vehicle archived = vehicles.archived("Ancienne", 120_000);
        vehicles.assign(user, archived);
        mockMvc.perform(as(INITIAL_ADMIN, get("/api/vehicles/" + archived.getId() + "/history")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(120000))
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(get("/api/vehicles/" + archived.getId() + "/history")))
                .andExpect(status().isForbidden());
    }
}
