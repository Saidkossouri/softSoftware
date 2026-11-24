package fr.carsupervision.member;

import static fr.carsupervision.support.TestMembers.google;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.carsupervision.api.model.MemberUpdateRequest;
import fr.carsupervision.common.error.ApiException;
import fr.carsupervision.common.error.ErrorCode;
import fr.carsupervision.support.ContractValidation;
import fr.carsupervision.support.IntegrationTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Il reste toujours au moins un ADMIN, y compris en cas d'actions simultanées (FR-012, SC-007). */
class LastAdminIT extends IntegrationTestBase {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    private UUID initialAdminId() {
        return memberRepository.findByEmail(INITIAL_ADMIN).orElseThrow().getId();
    }

    @Test
    void lastAdminCannotBeDemoted() throws Exception {
        mockMvc.perform(patch("/api/members/" + initialAdminId())
                        .with(google(INITIAL_ADMIN)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMIN"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(ContractValidation.conformsToContract());
        assertThat(memberRepository.countByRole(Role.ADMIN)).isEqualTo(1);
    }

    @Test
    void lastAdminCannotBeRemoved() throws Exception {
        mockMvc.perform(delete("/api/members/" + initialAdminId()).with(google(INITIAL_ADMIN)).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_ADMIN"))
                .andExpect(ContractValidation.conformsToContract());
        assertThat(memberRepository.count()).isEqualTo(1);
    }

    @Test
    void adminCanStepDownWhenAnotherAdminExists() {
        members.create("epouse@example.com", Role.ADMIN);

        memberService.update(initialAdminId(), new MemberUpdateRequest().role(fr.carsupervision.api.model.Role.USER));

        assertThat(memberRepository.countByRole(Role.ADMIN)).isEqualTo(1);
    }

    @Test
    void concurrentDemotionsOfTheTwoLastAdminsLeaveExactlyOneAdmin() throws Exception {
        UUID said = initialAdminId();
        UUID leila = members.create("epouse@example.com", Role.ADMIN).getId();
        assertConcurrentRunLeavesOneAdmin(
                () -> memberService.update(said, new MemberUpdateRequest().role(fr.carsupervision.api.model.Role.USER)),
                () -> memberService.update(leila, new MemberUpdateRequest().role(fr.carsupervision.api.model.Role.USER)));
    }

    @Test
    void concurrentRemovalsOfTheTwoLastAdminsLeaveExactlyOneAdmin() throws Exception {
        UUID said = initialAdminId();
        UUID leila = members.create("epouse@example.com", Role.ADMIN).getId();
        assertConcurrentRunLeavesOneAdmin(
                () -> {
                    memberService.delete(said);
                    return null;
                },
                () -> {
                    memberService.delete(leila);
                    return null;
                });
    }

    private void assertConcurrentRunLeavesOneAdmin(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Callable<?> action : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return action.call();
                }));
            }
            start.countDown();

            int successes = 0;
            int refusals = 0;
            for (Future<?> future : futures) {
                try {
                    future.get();
                    successes++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ApiException.class);
                    assertThat(((ApiException) e.getCause()).getCode()).isEqualTo(ErrorCode.LAST_ADMIN);
                    refusals++;
                }
            }
            assertThat(successes).isEqualTo(1);
            assertThat(refusals).isEqualTo(1);
            assertThat(memberRepository.countByRole(Role.ADMIN)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
