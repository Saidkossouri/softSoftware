package fr.carsupervision.maintenance;

import static fr.carsupervision.support.ContractValidation.conformsToContract;
import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestVehicles.reading;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import fr.carsupervision.member.Member;
import fr.carsupervision.member.Role;
import fr.carsupervision.support.IntegrationTestBase;
import fr.carsupervision.vehicle.Vehicle;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** US1 : relevés kilométriques (US1-1 à US1-5, FR-005 à FR-008). L'horloge est au 15/06/2026. */
class MileageReadingsIT extends IntegrationTestBase {

    private static final String USER = "fils@example.com";
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private Vehicle vehicle;

    @BeforeEach
    void vehicleAssignedToUser() {
        Member user = members.create(USER, Role.USER);
        vehicle = vehicles.active("A", 50_000);
        vehicles.assign(user, vehicle);
    }

    private ResultActions postReading(String email, String body) throws Exception {
        return mockMvc.perform(as(email, json(post("/api/vehicles/" + vehicle.getId() + "/mileage-readings"), body)));
    }

    private String create(LocalDate date, int km) throws Exception {
        String body = postReading(USER, reading(date, km)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void recordsReadingAndUpdatesCurrentMileage() throws Exception {
        postReading(USER, """
                {"readingDate":"2026-06-15","mileageKm":51200,"note":"  Plein  "}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.vehicleId").value(vehicle.getId().toString()))
                .andExpect(jsonPath("$.readingDate").value("2026-06-15"))
                .andExpect(jsonPath("$.mileageKm").value(51200))
                .andExpect(jsonPath("$.note").value("Plein"))
                .andExpect(jsonPath("$.authorName").value(USER))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(51200))
                .andExpect(jsonPath("$.currentMileage.date").value("2026-06-15"))
                .andExpect(conformsToContract());

        mockMvc.perform(as(USER, org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/vehicles/" + vehicle.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(51200))
                .andExpect(conformsToContract());
    }

    @Test
    void refusesLowerMileageAndCitesBlockingReading() throws Exception {
        create(TODAY.minusDays(5), 51_200);

        postReading(USER, reading(TODAY, 50_900))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MILEAGE_INCONSISTENT"))
                .andExpect(jsonPath("$.detail").value(containsString("51 200 km a été saisi le 10/06/2026")))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("mileageKm"))
                .andExpect(conformsToContract());
    }

    @Test
    void acceptsConsistentBackdatedReadingWithoutChangingCurrentMileage() throws Exception {
        create(TODAY, 52_000);

        postReading(USER, reading(TODAY.minusDays(30), 51_000))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(52000))
                .andExpect(jsonPath("$.currentMileage.date").value("2026-06-15"))
                .andExpect(conformsToContract());
    }

    @Test
    void acceptsTwoReadingsOnTheSameDay() throws Exception {
        create(TODAY, 51_500);

        postReading(USER, reading(TODAY, 51_000)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(51500));
    }

    @Test
    void refusesMileageBelowInitialMileage() throws Exception {
        postReading(USER, reading(TODAY, 49_999))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MILEAGE_INCONSISTENT"))
                .andExpect(jsonPath("$.detail").value(containsString("50 000 km")))
                .andExpect(conformsToContract());
    }

    @Test
    void refusesFutureDateUsingMoroccanDay() throws Exception {
        postReading(USER, reading(TODAY.plusDays(1), 51_000))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("readingDate"))
                .andExpect(conformsToContract());

        // 23 h 30 UTC = 0 h 30 le lendemain au Maroc (UTC+1) : ce lendemain n'est pas dans le futur.
        clock.set(Instant.parse("2026-06-15T23:30:00Z"));
        postReading(USER, reading(TODAY.plusDays(1), 51_000)).andExpect(status().isCreated());
    }

    @Test
    void asksConfirmationForJumpAboveOneHundredThousandKilometres() throws Exception {
        create(TODAY.minusDays(1), 51_200);

        postReading(USER, reading(TODAY, 251_300))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MILEAGE_JUMP_NOT_CONFIRMED"))
                .andExpect(conformsToContract());

        postReading(USER, reading(TODAY, 251_300, true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(251300));
    }

    @Test
    void correctionAndDeletionRecomputeCurrentMileage() throws Exception {
        create(TODAY.minusDays(10), 51_000);
        String latest = create(TODAY, 52_000);

        mockMvc.perform(as(USER, json(put("/api/mileage-readings/" + latest), reading(TODAY, 52_500))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mileageKm").value(52500))
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(52500))
                .andExpect(conformsToContract());

        // Une correction est contrôlée sans tenir compte de la valeur qu'elle remplace.
        mockMvc.perform(as(USER, json(put("/api/mileage-readings/" + latest), reading(TODAY, 50_500))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MILEAGE_INCONSISTENT"));

        mockMvc.perform(as(USER, delete("/api/mileage-readings/" + latest)))
                .andExpect(status().isNoContent());
        mockMvc.perform(as(USER, org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/vehicles/" + vehicle.getId())))
                .andExpect(jsonPath("$.currentMileage.mileageKm").value(51000))
                .andExpect(jsonPath("$.currentMileage.date").value("2026-06-05"));
    }

    @Test
    void deletingLastReadingRestoresInitialMileage() throws Exception {
        String only = create(TODAY, 51_000);

        mockMvc.perform(as(USER, delete("/api/mileage-readings/" + only))).andExpect(status().isNoContent());

        Vehicle reloaded = vehicles.reload(vehicle);
        org.assertj.core.api.Assertions.assertThat(reloaded.getCurrentMileageKm()).isEqualTo(50_000);
        org.assertj.core.api.Assertions.assertThat(reloaded.getCurrentMileageDate()).isEqualTo(TODAY);
    }
}
