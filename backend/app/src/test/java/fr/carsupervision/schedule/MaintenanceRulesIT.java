package fr.carsupervision.schedule;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestCatalogue.ASSURANCE;
import static fr.carsupervision.support.TestCatalogue.CLIMATISATION;
import static fr.carsupervision.support.TestCatalogue.FILTRE_A_HUILE;
import static fr.carsupervision.support.TestCatalogue.VIDANGE;
import static fr.carsupervision.support.TestCatalogue.record;
import static fr.carsupervision.support.TestVehicles.reading;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import fr.carsupervision.vehicle.Vehicle;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** US4 : règles d'échéance de bout en bout (US4-1 à US4-5, FR-021 à FR-026). Horloge au 15/06/2026. */
class MaintenanceRulesIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private Vehicle vehicle;

    @BeforeEach
    void vehicleAssignedToUser() {
        Member user = members.create(USER, Role.USER);
        vehicle = vehicles.active("A", 40_000);
        vehicles.assign(user, vehicle);
    }

    private ResultActions postRule(String email, String body) throws Exception {
        return mockMvc.perform(as(email, json(post("/api/vehicles/" + vehicle.getId() + "/maintenance-rules"),
                body)));
    }

    private String createRule(String email, String body) throws Exception {
        return JsonPath.read(postRule(email, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions dueItems(String email) throws Exception {
        return mockMvc.perform(as(email, get("/api/vehicles/" + vehicle.getId() + "/due-items")));
    }

    private void addReading(String date, int km) throws Exception {
        mockMvc.perform(as(USER, json(post("/api/vehicles/" + vehicle.getId() + "/mileage-readings"),
                reading(LocalDate.parse(date), km)))).andExpect(status().isCreated());
    }

    private String addRecord(String date, int km, UUID... types) throws Exception {
        String body = mockMvc.perform(as(USER, json(post("/api/vehicles/" + vehicle.getId() + "/maintenance-records"),
                record(date, km, null, types)))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void createsRuleWithTodayAndCurrentMileageAsDefaultBaseline() throws Exception {
        postRule(USER, """
                {"typeId":"%s","intervalKm":15000,"intervalMonths":12}""".formatted(VIDANGE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type.name").value("Vidange"))
                .andExpect(jsonPath("$.intervalKm").value(15000))
                .andExpect(jsonPath("$.intervalMonths").value(12))
                .andExpect(jsonPath("$.baselineDate").value("2026-06-15"))
                .andExpect(jsonPath("$.baselineMileageKm").value(40000))
                .andExpect(conformsToContract());

        dueItems(USER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].kind").value("RULE"))
                .andExpect(jsonPath("$[0].status").value("OK"))
                .andExpect(jsonPath("$[0].dueDate").value("2027-06-15"))
                .andExpect(jsonPath("$[0].dueMileageKm").value(55000))
                .andExpect(jsonPath("$[0].remainingKm").value(15000))
                .andExpect(jsonPath("$[0].lastDoneDate").value("2026-06-15"))
                .andExpect(jsonPath("$[0].lastDoneRecordId").doesNotExist())
                .andExpect(conformsToContract());
    }

    /** US4-1 à US4-5 : « Bientôt », « En retard », relance, entretien rétroactif, suppression. */
    @Test
    void recomputesAfterEachEntry() throws Exception {
        createRule(USER, """
                {"typeId":"%s","intervalKm":15000,"intervalMonths":12,"baselineDate":"2025-09-15",
                 "baselineMileageKm":45000}""".formatted(VIDANGE));

        addReading("2026-06-15", 59_200);
        dueItems(USER)
                .andExpect(jsonPath("$[0].status").value("SOON"))
                .andExpect(jsonPath("$[0].remainingKm").value(800))
                .andExpect(jsonPath("$[0].remainingDays").value(92))
                .andExpect(jsonPath("$[0].lastDoneMileageKm").value(45000));

        addReading("2026-06-15", 60_400);
        dueItems(USER)
                .andExpect(jsonPath("$[0].status").value("OVERDUE"))
                .andExpect(jsonPath("$[0].remainingKm").value(-400))
                .andExpect(conformsToContract());

        String vidange = addRecord("2026-06-15", 60_400, VIDANGE, FILTRE_A_HUILE);
        dueItems(USER)
                .andExpect(jsonPath("$[0].status").value("OK"))
                .andExpect(jsonPath("$[0].lastDoneRecordId").value(vidange))
                .andExpect(jsonPath("$[0].lastDoneDate").value("2026-06-15"))
                .andExpect(jsonPath("$[0].dueMileageKm").value(75400))
                .andExpect(jsonPath("$[0].dueDate").value("2027-06-15"));

        String older = addRecord("2025-12-01", 50_000, VIDANGE);
        dueItems(USER).andExpect(jsonPath("$[0].lastDoneRecordId").value(vidange));

        mockMvc.perform(as(USER, delete("/api/maintenance-records/" + vidange))).andExpect(status().isNoContent());
        dueItems(USER)
                .andExpect(jsonPath("$[0].lastDoneRecordId").value(older))
                .andExpect(jsonPath("$[0].dueMileageKm").value(65000))
                .andExpect(jsonPath("$[0].dueDate").value("2026-12-01"))
                .andExpect(jsonPath("$[0].status").value("OK"));

        mockMvc.perform(as(USER, delete("/api/maintenance-records/" + older))).andExpect(status().isNoContent());
        dueItems(USER)
                .andExpect(jsonPath("$[0].lastDoneRecordId").doesNotExist())
                .andExpect(jsonPath("$[0].lastDoneDate").value("2025-09-15"))
                .andExpect(jsonPath("$[0].status").value("OVERDUE"));
    }

    @Test
    void recordOlderThanBaselineIsIgnored() throws Exception {
        addRecord("2026-01-10", 45_000, VIDANGE);
        createRule(USER, """
                {"typeId":"%s","intervalKm":10000,"baselineDate":"2026-03-01","baselineMileageKm":47000}"""
                .formatted(VIDANGE));

        dueItems(USER)
                .andExpect(jsonPath("$[0].lastDoneRecordId").doesNotExist())
                .andExpect(jsonPath("$[0].dueMileageKm").value(57000))
                .andExpect(jsonPath("$[0].dueDate").doesNotExist())
                .andExpect(jsonPath("$[0].remainingDays").doesNotExist());
    }

    @Test
    void validatesRules() throws Exception {
        postRule(USER, """
                {"typeId":"%s"}""".formatted(VIDANGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("intervalKm"))
                .andExpect(conformsToContract());
        postRule(USER, """
                {"typeId":"%s","intervalMonths":12}""".formatted(ASSURANCE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("typeId"));
        postRule(USER, """
                {"typeId":"%s","intervalMonths":12,"baselineDate":"2026-06-16"}""".formatted(VIDANGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("baselineDate"));
        postRule(USER, """
                {"typeId":"%s","intervalKm":0}""".formatted(VIDANGE))
                .andExpect(status().isBadRequest());

        jdbc.update("UPDATE maintenance_type SET active = false WHERE id = ?", CLIMATISATION);
        postRule(USER, """
                {"typeId":"%s","intervalMonths":24}""".formatted(CLIMATISATION))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("typeId"));

        createRule(USER, """
                {"typeId":"%s","intervalKm":15000}""".formatted(VIDANGE));
        postRule(INITIAL_ADMIN, """
                {"typeId":"%s","intervalKm":10000}""".formatted(VIDANGE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RULE_ALREADY_EXISTS"))
                .andExpect(conformsToContract());
    }

    @Test
    void catalogueFrequencyChangeDoesNotAffectExistingRule() throws Exception {
        createRule(USER, """
                {"typeId":"%s","intervalKm":15000}""".formatted(VIDANGE));
        jdbc.update("UPDATE maintenance_type SET default_interval_km = 10000 WHERE id = ?", VIDANGE);

        dueItems(USER)
                .andExpect(jsonPath("$[0].intervalKm").value(15000))
                .andExpect(jsonPath("$[0].dueMileageKm").value(55000));
    }

    @Test
    void anyMemberWithAccessUpdatesAndDeletes() throws Exception {
        String ruleId = createRule(INITIAL_ADMIN, """
                {"typeId":"%s","intervalKm":15000,"intervalMonths":12}""".formatted(VIDANGE));

        mockMvc.perform(as(USER, json(put("/api/maintenance-rules/" + ruleId), """
                {"intervalMonths":6,"baselineDate":"2026-06-01","baselineMileageKm":39000}""")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type.name").value("Vidange"))
                .andExpect(jsonPath("$.intervalKm").doesNotExist())
                .andExpect(jsonPath("$.intervalMonths").value(6))
                .andExpect(jsonPath("$.baselineDate").value("2026-06-01"))
                .andExpect(conformsToContract());
        dueItems(USER).andExpect(jsonPath("$[0].dueDate").value("2026-12-01"));

        mockMvc.perform(as(USER, delete("/api/maintenance-rules/" + ruleId))).andExpect(status().isNoContent());
        dueItems(USER).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void accessAndArchive() throws Exception {
        String ruleId = createRule(USER, """
                {"typeId":"%s","intervalKm":15000}""".formatted(VIDANGE));
        members.create("autre@example.com", Role.USER);

        postRule("autre@example.com", """
                {"typeId":"%s","intervalKm":15000}""".formatted(FILTRE_A_HUILE))
                .andExpect(status().isForbidden())
                .andExpect(conformsToContract());
        dueItems("autre@example.com").andExpect(status().isForbidden());
        mockMvc.perform(as("autre@example.com", delete("/api/maintenance-rules/" + ruleId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(INITIAL_ADMIN, delete("/api/maintenance-rules/" + UUID.randomUUID())))
                .andExpect(status().isNotFound());

        jdbc.update("UPDATE vehicle SET status = 'ARCHIVED' WHERE id = ?", vehicle.getId());
        postRule(INITIAL_ADMIN, """
                {"typeId":"%s","intervalKm":15000}""".formatted(FILTRE_A_HUILE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ARCHIVED"));
        mockMvc.perform(as(INITIAL_ADMIN, json(put("/api/maintenance-rules/" + ruleId), """
                {"intervalKm":10000,"baselineDate":"2026-06-01","baselineMileageKm":39000}""")))
                .andExpect(status().isConflict());
        dueItems(INITIAL_ADMIN).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        dueItems(USER).andExpect(status().isForbidden());
    }
}
