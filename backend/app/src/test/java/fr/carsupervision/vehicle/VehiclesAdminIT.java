package fr.carsupervision.vehicle;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.TestMembers.google;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import java.time.Year;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Histoire 3 : gestion du parc de véhicules par un ADMIN (FR-014 à FR-018). */
class VehiclesAdminIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";

    private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return request.with(google(INITIAL_ADMIN)).with(csrf());
    }

    private static MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request) {
        return request.with(google(USER)).with(csrf());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static String input(String nickname, String registration) {
        return """
                {"nickname":"%s","make":"Peugeot","model":"308","registration":"%s","initialMileageKm":12000}
                """.formatted(nickname, registration);
    }

    private String createVehicle(String nickname, String registration) throws Exception {
        String body = mockMvc.perform(asAdmin(json(post("/api/vehicles"), input(nickname, registration))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void createsActiveVehicleWithUppercaseRegistration() throws Exception {
        mockMvc.perform(asAdmin(json(post("/api/vehicles"), """
                        {"nickname":"  La familiale ","make":"Peugeot","model":"5008","registration":" ab-123-cd ",
                         "firstRegistrationYear":2019,"initialMileageKm":45000,"vin":"vf3mcyhzrks123456",
                         "energyType":"DIESEL"}
                        """)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nickname").value("La familiale"))
                .andExpect(jsonPath("$.registration").value("AB-123-CD"))
                .andExpect(jsonPath("$.firstRegistrationYear").value(2019))
                .andExpect(jsonPath("$.initialMileageKm").value(45000))
                .andExpect(jsonPath("$.vin").value("VF3MCYHZRKS123456"))
                .andExpect(jsonPath("$.energyType").value("DIESEL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(conformsToContract());
    }

    @Test
    void updatesAllFields() throws Exception {
        String id = createVehicle("Citadine", "AA-111-AA");

        mockMvc.perform(asAdmin(json(put("/api/vehicles/" + id), """
                        {"nickname":"Voiture de Leila","make":"Renault","model":"Clio","registration":"bb 222 bb",
                         "firstRegistrationYear":2021,"initialMileageKm":3000,"vin":"VF1RJA00012345678",
                         "energyType":"HYBRID"}
                        """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Voiture de Leila"))
                .andExpect(jsonPath("$.make").value("Renault"))
                .andExpect(jsonPath("$.model").value("Clio"))
                .andExpect(jsonPath("$.registration").value("BB 222 BB"))
                .andExpect(jsonPath("$.firstRegistrationYear").value(2021))
                .andExpect(jsonPath("$.initialMileageKm").value(3000))
                .andExpect(jsonPath("$.vin").value("VF1RJA00012345678"))
                .andExpect(jsonPath("$.energyType").value("HYBRID"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(conformsToContract());

        // Les champs facultatifs absents sont effacés (remplacement complet).
        mockMvc.perform(asAdmin(json(put("/api/vehicles/" + id), input("Voiture de Leila", "BB-222-BB"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vin").doesNotExist())
                .andExpect(jsonPath("$.energyType").doesNotExist())
                .andExpect(jsonPath("$.firstRegistrationYear").doesNotExist())
                .andExpect(conformsToContract());
    }

    @Test
    void refusesDuplicateRegistrationWrittenDifferently() throws Exception {
        createVehicle("Première", "AB123CD");

        mockMvc.perform(asAdmin(json(post("/api/vehicles"), input("Seconde", "ab-123 cd"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_ALREADY_EXISTS"))
                .andExpect(conformsToContract());
    }

    @Test
    void refusesRegistrationOfArchivedVehicle() throws Exception {
        String archived = createVehicle("Ancienne", "AB123CD");
        mockMvc.perform(asAdmin(post("/api/vehicles/" + archived + "/archive"))).andExpect(status().isOk());
        String other = createVehicle("Autre", "ZZ-999-ZZ");

        mockMvc.perform(asAdmin(json(post("/api/vehicles"), input("Nouvelle", "ab 123 cd"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_ALREADY_EXISTS"));
        mockMvc.perform(asAdmin(json(put("/api/vehicles/" + other), input("Autre", "AB-123-CD"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REGISTRATION_ALREADY_EXISTS"))
                .andExpect(conformsToContract());

        // Garder sa propre immatriculation (écrite autrement) n'est pas un doublon.
        mockMvc.perform(asAdmin(json(put("/api/vehicles/" + archived), input("Ancienne", "ab-123-cd"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void refusesBlankRequiredFields() throws Exception {
        mockMvc.perform(asAdmin(json(post("/api/vehicles"), """
                        {"nickname":"  ","make":" ","model":"308","registration":" - ","initialMileageKm":0}
                        """)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("nickname", "make", "registration")));
    }

    @Test
    void refusesMissingRequiredFields() throws Exception {
        mockMvc.perform(asAdmin(json(post("/api/vehicles"), "{\"nickname\":\"A\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("make", "model", "registration", "initialMileageKm")));
    }

    @Test
    void refusesOutOfRangeMileage() throws Exception {
        for (int mileage : new int[] {-1, 2_000_001}) {
            mockMvc.perform(asAdmin(json(post("/api/vehicles"), """
                            {"nickname":"A","make":"B","model":"C","registration":"AB123CD","initialMileageKm":%d}
                            """.formatted(mileage))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("initialMileageKm"));
        }
    }

    @Test
    void refusesYearBefore1900OrInTheFuture() throws Exception {
        for (int year : new int[] {1899, Year.now().getValue() + 1}) {
            mockMvc.perform(asAdmin(json(post("/api/vehicles"), """
                            {"nickname":"A","make":"B","model":"C","registration":"AB123CD",
                             "initialMileageKm":0,"firstRegistrationYear":%d}
                            """.formatted(year))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("firstRegistrationYear"));
        }
    }

    @Test
    void refusesInvalidVin() throws Exception {
        for (String vin : new String[] {"VF3MCYHZRKS12345", "VF3MCYHZRKS12345O", "VF3MCYHZRKS1234-6"}) {
            mockMvc.perform(asAdmin(json(post("/api/vehicles"), """
                            {"nickname":"A","make":"B","model":"C","registration":"AB123CD",
                             "initialMileageKm":0,"vin":"%s"}
                            """.formatted(vin))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("vin"));
        }
    }

    @Test
    void archivesAndReactivatesIdempotently() throws Exception {
        String id = createVehicle("Citadine", "AA-111-AA");

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(asAdmin(post("/api/vehicles/" + id + "/archive")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARCHIVED"))
                    .andExpect(conformsToContract());
        }
        mockMvc.perform(asAdmin(get("/api/vehicles")))
                .andExpect(jsonPath("$", hasSize(0)));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(asAdmin(post("/api/vehicles/" + id + "/reactivate")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(conformsToContract());
        }
        mockMvc.perform(asAdmin(get("/api/vehicles/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(conformsToContract());
    }

    @Test
    void listsActiveByDefaultAndArchivedOnRequestSortedByNickname() throws Exception {
        createVehicle("Zoé", "AA-111-AA");
        createVehicle("Berline", "BB-222-BB");
        String archived = createVehicle("Ancienne", "CC-333-CC");
        mockMvc.perform(asAdmin(post("/api/vehicles/" + archived + "/archive"))).andExpect(status().isOk());

        mockMvc.perform(asAdmin(get("/api/vehicles")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nickname", contains("Berline", "Zoé")))
                .andExpect(conformsToContract());
        mockMvc.perform(asAdmin(get("/api/vehicles").param("status", "ARCHIVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nickname", contains("Ancienne")))
                .andExpect(conformsToContract());
    }

    @Test
    void unknownVehicleReturns404ForAdmin() throws Exception {
        String unknown = "/api/vehicles/" + UUID.randomUUID();
        mockMvc.perform(asAdmin(get(unknown)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(conformsToContract());
        mockMvc.perform(asAdmin(json(put(unknown), input("A", "AB123CD"))))
                .andExpect(status().isNotFound())
                .andExpect(conformsToContract());
        mockMvc.perform(asAdmin(post(unknown + "/archive")))
                .andExpect(status().isNotFound())
                .andExpect(conformsToContract());
        mockMvc.perform(asAdmin(post(unknown + "/reactivate")))
                .andExpect(status().isNotFound())
                .andExpect(conformsToContract());
    }

    @Test
    void userCannotWriteTheFleet() throws Exception {
        members.create(USER, Role.USER);
        String id = createVehicle("Citadine", "AA-111-AA");

        mockMvc.perform(asUser(json(post("/api/vehicles"), input("A", "ZZ123ZZ"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_ONLY"))
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(json(put("/api/vehicles/" + id), input("A", "AA-111-AA"))))
                .andExpect(status().isForbidden())
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(post("/api/vehicles/" + id + "/archive")))
                .andExpect(status().isForbidden())
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(post("/api/vehicles/" + id + "/reactivate")))
                .andExpect(status().isForbidden())
                .andExpect(conformsToContract());

        mockMvc.perform(asAdmin(get("/api/vehicles/" + id)))
                .andExpect(jsonPath("$.nickname").value("Citadine"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void userWithoutAccessSeesNoVehicle() throws Exception {
        members.create(USER, Role.USER);
        String id = createVehicle("Citadine", "AA-111-AA");

        mockMvc.perform(asUser(get("/api/vehicles")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)))
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(get("/api/vehicles").param("status", "ARCHIVED")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"))
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(get("/api/vehicles/" + id)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"))
                .andExpect(conformsToContract());
        mockMvc.perform(asUser(get("/api/vehicles/" + UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VEHICLE_FORBIDDEN"));
    }
}
