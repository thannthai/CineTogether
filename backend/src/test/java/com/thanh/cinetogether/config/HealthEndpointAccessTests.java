package com.thanh.cinetogether.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.thanh.cinetogether.TestcontainersConfiguration;

/** The hosting platform's health check has no token: only the health status is public, and it shows no details. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointAccessTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPublicAndShowsOnlyTheStatus() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    /** Uptime monitors check with HEAD, which is the lighter request. */
    @Test
    void healthAnswersAHeadRequestWithoutAToken() throws Exception {
        mockMvc.perform(head("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void aHeadRequestToAnotherActuatorEndpointStillNeedsAToken() throws Exception {
        mockMvc.perform(head("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void everyOtherActuatorEndpointNeedsAToken() throws Exception {
        for (String path : new String[] { "/actuator", "/actuator/env", "/actuator/beans", "/actuator/info" }) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

}
