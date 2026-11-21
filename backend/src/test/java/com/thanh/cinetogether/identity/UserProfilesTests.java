package com.thanh.cinetogether.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.thanh.cinetogether.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserProfilesTests {

    @Autowired
    private UserProfiles userProfiles;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void firstCallCreatesProfileWithGivenName() {
        UUID userId = UUID.randomUUID();

        userProfiles.createIfAbsent(userId, "Minh");

        List<Map<String, Object>> rows = rowsFor(userId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("display_name", "Minh");
        assertThat(rows.get(0).get("created_at")).isNotNull().isEqualTo(rows.get(0).get("updated_at"));
    }

    @Test
    void secondCallLeavesExistingProfileUnchanged() {
        UUID userId = UUID.randomUUID();
        userProfiles.createIfAbsent(userId, "Minh");
        List<Map<String, Object>> before = rowsFor(userId);

        // The name may have been edited by the user since the first call.
        userProfiles.createIfAbsent(userId, "Another Name");

        assertThat(rowsFor(userId)).isEqualTo(before);
    }

    @Test
    void concurrentCallsForTheSameUserCreateExactlyOneProfile() throws Exception {
        // One race is easy to miss, so repeat it with a fresh user each round.
        for (int round = 0; round < 20; round++) {
            UUID userId = UUID.randomUUID();

            callConcurrently(userId, 8);

            assertThat(rowsFor(userId)).hasSize(1);
        }
    }

    private void callConcurrently(UUID userId, int threads) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> calls = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                String name = "Name-" + i;
                calls.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    userProfiles.createIfAbsent(userId, name);
                    return null;
                }));
            }
            // Release all threads at once to make a real race likely.
            assertThat(ready.await(10, TimeUnit.SECONDS)).as("all workers should be ready").isTrue();
            start.countDown();
            for (Future<?> call : calls) {
                call.get(10, TimeUnit.SECONDS); // rethrows a failure of any call
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private List<Map<String, Object>> rowsFor(UUID userId) {
        return jdbc.sql("SELECT display_name, created_at, updated_at FROM app_users WHERE id = :id")
                .param("id", userId)
                .query()
                .listOfRows();
    }

}
