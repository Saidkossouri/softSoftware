package fr.carsupervision.schedule;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestCatalogue.ASSURANCE;
import static fr.carsupervision.support.TestCatalogue.VIDANGE;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** US4 : échéances à date fixe (US4-6, FR-036 à FR-039). Horloge au 15/06/2026. */
class DeadlinesIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private Vehicle vehicle;

    @BeforeEach
    void vehicleAssignedToUser() {
        Member user = members.create(USER, Role.USER);
        vehicle = vehicles.active("A", 40_000);
        vehicles.assign(user, vehicle);
    }

    private ResultActions postDeadline(String email, String body) throws Exception {
        return mockMvc.perform(as(email, json(post("/api/vehicles/" + vehicle.getId() + "/deadlines"), body)));
    }

    private String createDeadline(String body) throws Exception {
        return JsonPath.read(postDeadline(USER, body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private ResultActions complete(String email, String id) throws Exception {
        return mockMvc.perform(as(email, post("/api/deadlines/" + id + "/complete")));
    }

    private ResultActions dueItems() throws Exception {
        return mockMvc.perform(as(USER, get("/api/vehicles/" + vehicle.getId() + "/due-items")));
    }

    @Test
    void createsFromTypeOrFreeLabelAndComputesState() throws Exception {
        postDeadline(USER, """
                {"typeId":"%s","dueDate":"2026-07-01","recurrenceMonths":12}""".formatted(ASSURANCE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Assurance"))
                .andExpect(jsonPath("$.type.name").value("Assurance"))
                .andExpect(jsonPath("$.recurrenceMonths").value(12))
                .andExpect(jsonPath("$.done").value(false))
                .andExpect(conformsToContract());
        postDeadline(USER, """
                {"label":"  Carte grise  ","dueDate":"2026-05-01"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Carte grise"))
                .andExpect(jsonPath("$.type").doesNotExist());
        postDeadline(USER, """
                {"typeId":"%s","label":"Assurance Wafa","dueDate":"2026-12-31"}""".formatted(ASSURANCE))
                .andExpect(jsonPath("$.label").value("Assurance Wafa"));

        dueItems()
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].kind").value("DEADLINE"))
                .andExpect(jsonPath("$[0].label").value("Carte grise"))
                .andExpect(jsonPath("$[0].status").value("OVERDUE"))
                .andExpect(jsonPath("$[0].remainingDays").value(-45))
                .andExpect(jsonPath("$[1].label").value("Assurance"))
                .andExpect(jsonPath("$[1].status").value("SOON"))
                .andExpect(jsonPath("$[1].remainingDays").value(16))
                .andExpect(jsonPath("$[1].recurrenceMonths").value(12))
                .andExpect(jsonPath("$[2].status").value("OK"))
                .andExpect(conformsToContract());
    }

    @Test
    void validatesDeadlines() throws Exception {
        postDeadline(USER, """
                {"typeId":"%s","dueDate":"2026-07-01"}""".formatted(VIDANGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("typeId"))
                .andExpect(conformsToContract());
        postDeadline(USER, """
                {"label":"   ","dueDate":"2026-07-01"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("label"));
        postDeadline(USER, """
                {"label":"Vignette","dueDate":"2026-07-01","recurrenceMonths":0}""")
                .andExpect(status().isBadRequest());
    }

    /** US4-6 : l'assurance du 1er avril 2027 faite donne celle du 1er avril 2028. */
    @Test
    void completingRecurringDeadlineCreatesNextOne() throws Exception {
        String id = createDeadline("""
                {"typeId":"%s","dueDate":"2027-04-01","recurrenceMonths":12}""".formatted(ASSURANCE));

        String body = complete(USER, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed.done").value(true))
                .andExpect(jsonPath("$.completed.doneAt").exists())
                .andExpect(jsonPath("$.next.dueDate").value("2028-04-01"))
                .andExpect(jsonPath("$.next.label").value("Assurance"))
                .andExpect(jsonPath("$.next.recurrenceMonths").value(12))
                .andExpect(jsonPath("$.next.done").value(false))
                .andExpect(conformsToContract())
                .andReturn().getResponse().getContentAsString();
        String nextId = JsonPath.read(body, "$.next.id");

        complete(INITIAL_ADMIN, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed.done").value(true))
                .andExpect(jsonPath("$.next").doesNotExist());
        dueItems()
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deadlineId").value(nextId));
    }

    @Test
    void completingSingleDeadlineLeavesNothing() throws Exception {
        String id = createDeadline("""
                {"label":"Contrôle pollution","dueDate":"2026-06-20"}""");

        complete(USER, id).andExpect(status().isOk()).andExpect(jsonPath("$.next").doesNotExist());
        dueItems().andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void updatesAndDeletes() throws Exception {
        String id = createDeadline("""
                {"typeId":"%s","dueDate":"2026-07-01","recurrenceMonths":12}""".formatted(ASSURANCE));
        jdbc.update("UPDATE maintenance_type SET active = false WHERE id = ?", ASSURANCE);

        mockMvc.perform(as(INITIAL_ADMIN, json(put("/api/deadlines/" + id), """
                {"typeId":"%s","dueDate":"2026-08-01"}""".formatted(ASSURANCE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").value("2026-08-01"))
                .andExpect(jsonPath("$.recurrenceMonths").doesNotExist())
                .andExpect(jsonPath("$.type.active").value(false))
                .andExpect(conformsToContract());
        mockMvc.perform(as(USER, delete("/api/deadlines/" + id))).andExpect(status().isNoContent());
        dueItems().andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void accessAndArchive() throws Exception {
        String id = createDeadline("""
                {"label":"Vignette","dueDate":"2027-01-31","recurrenceMonths":12}""");
        members.create("autre@example.com", Role.USER);

        postDeadline("autre@example.com", """
                {"label":"Vignette","dueDate":"2027-01-31"}""")
                .andExpect(status().isForbidden());
        complete("autre@example.com", id).andExpect(status().isForbidden()).andExpect(conformsToContract());

        jdbc.update("UPDATE vehicle SET status = 'ARCHIVED' WHERE id = ?", vehicle.getId());
        postDeadline(INITIAL_ADMIN, """
                {"label":"Vignette","dueDate":"2027-01-31"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ARCHIVED"));
        complete(INITIAL_ADMIN, id).andExpect(status().isConflict()).andExpect(conformsToContract());
    }
}
