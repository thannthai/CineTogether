package com.thanh.cinetogether.identity.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.thanh.cinetogether.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MeEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void useTheSystemTimeUnlessATestSetsOne() {
        given(clock.instant()).willAnswer(invocation -> Instant.now());
    }

    @Test
    void getMeReturnsTheCallersProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        tokenFor("token-a", userId, "Minh Nguyen");

        getMe("token-a")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.displayName").value("Minh Nguyen"))
                .andExpect(jsonPath("$.createdAt", matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void patchMeRenamesTheCallerAndReturnsTheUpdatedProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        tokenFor("token-a", userId, "Minh");

        patchMe("token-a", "{\"displayName\": \"  Minh Nguyen  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.displayName").value("Minh Nguyen"))
                .andExpect(jsonPath("$.length()").value(3));

        getMe("token-a").andExpect(jsonPath("$.displayName").value("Minh Nguyen"));
    }

    @Test
    void patchMeSetsUpdatedAtFromTheClockAndKeepsCreatedAt() throws Exception {
        UUID userId = UUID.randomUUID();
        tokenFor("token-a", userId, "Minh");
        getMe("token-a");
        Instant createdAtBefore = timestamp(userId, "created_at");
        Instant later = Instant.parse("2030-01-01T00:00:00Z");
        given(clock.instant()).willReturn(later);

        patchMe("token-a", "{\"displayName\": \"Minh Nguyen\"}").andExpect(status().isOk());

        assertThat(timestamp(userId, "updated_at")).isEqualTo(later);
        assertThat(timestamp(userId, "created_at")).isEqualTo(createdAtBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"displayName\": \"   \"}", "{}"})
    void patchMeWithAnInvalidNameIsRejectedWithValidationError(String body) throws Exception {
        tokenFor("token-a", UUID.randomUUID(), "Minh");

        assertValidationError(patchMe("token-a", body));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{not json"})
    void patchMeWithAnUnreadableBodyIsRejectedWithValidationError(String body) throws Exception {
        tokenFor("token-a", UUID.randomUUID(), "Minh");

        assertValidationError(patchMe("token-a", body));
    }

    @Test
    void rejectedPatchLeavesTheProfileUnchanged() throws Exception {
        UUID userId = UUID.randomUUID();
        tokenFor("token-a", userId, "Minh");
        getMe("token-a");
        Instant updatedAtBefore = timestamp(userId, "updated_at");

        patchMe("token-a", "{\"displayName\": \"   \"}").andExpect(status().isBadRequest());

        getMe("token-a").andExpect(jsonPath("$.displayName").value("Minh"));
        assertThat(timestamp(userId, "updated_at")).isEqualTo(updatedAtBefore);
    }

    @Test
    void patchMeIgnoresAnIdInTheBody() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        tokenFor("token-a", userA, "Minh");
        tokenFor("token-b", userB, "Lan");
        getMe("token-a");

        patchMe("token-b", "{\"id\": \"" + userA + "\", \"displayName\": \"Hacked\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userB.toString()));

        getMe("token-a").andExpect(jsonPath("$.displayName").value("Minh"));
    }

    @Test
    void anonymousGetMeIsRejectedWithUnauthenticatedProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    // The body is invalid on purpose: authentication must be checked before the body is read.
    @Test
    void anonymousPatchMeIsRejectedWithUnauthenticatedProblemJson() throws Exception {
        mockMvc.perform(patch("/api/v1/me").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    private void assertValidationError(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    private ResultActions getMe(String token) throws Exception {
        return mockMvc.perform(get("/api/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions patchMe(String token, String json) throws Exception {
        return mockMvc.perform(patch("/api/v1/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private Instant timestamp(UUID userId, String column) {
        return jdbc.sql("SELECT " + column + " FROM app_users WHERE id = :id")
                .param("id", userId)
                .query(OffsetDateTime.class)
                .single()
                .toInstant();
    }

    private void tokenFor(String token, UUID userId, String displayName) {
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "ES256")
                .subject(userId.toString())
                .claim("email", "user@example.com")
                .claim("user_metadata", Map.of("display_name", displayName))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        given(jwtDecoder.decode(token)).willReturn(jwt);
    }

}
