package fr.carsupervision.maintenance;

import static fr.carsupervision.support.Requests.as;
import static fr.carsupervision.support.Requests.json;
import static fr.carsupervision.support.TestVehicles.reading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import fr.carsupervision.support.IntegrationTestBase;
import fr.carsupervision.vehicle.Vehicle;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Verrou par véhicule (research R4) : deux saisies incompatibles simultanées, une seule passe. */
class MileageConcurrencyIT extends IntegrationTestBase {

    @Test
    void incompatibleConcurrentReadingsAreSerialized() throws Exception {
        Vehicle vehicle = vehicles.active("A", 50_000);
        String path = "/api/vehicles/" + vehicle.getId() + "/mileage-readings";
        // Chacune est valide seule ; ensemble, la plus tardive aurait un kilométrage plus petit.
        List<String> bodies = List.of(reading(LocalDate.of(2026, 6, 10), 60_000),
                reading(LocalDate.of(2026, 6, 12), 55_000));

        for (int attempt = 0; attempt < 5; attempt++) {
            jdbc.execute("DELETE FROM mileage_reading");
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                List<Future<Integer>> results = new ArrayList<>();
                for (String body : bodies) {
                    Callable<Integer> call = () -> {
                        start.await();
                        return mockMvc.perform(as(INITIAL_ADMIN, json(post(path), body)))
                                .andReturn().getResponse().getStatus();
                    };
                    results.add(executor.submit(call));
                }
                start.countDown();
                List<Integer> statuses = new ArrayList<>();
                for (Future<Integer> result : results) {
                    statuses.add(result.get());
                }
                assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
