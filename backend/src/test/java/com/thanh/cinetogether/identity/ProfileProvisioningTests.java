package com.thanh.cinetogether.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thanh.cinetogether.TestcontainersConfiguration;
import com.thanh.cinetogether.identity.persistence.UserProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ProfileProvisioningTests {

    private static final String TOKEN = "valid-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoSpyBean
    private UserProfiles userProfiles;

    @Test
    void authenticatedRequestCreatesProfileWithDisplayNameFromToken() throws Exception {
        UUID userId = UUID.randomUUID();
        tokenFor(userId, "minh@example.com", Map.of("display_name", "Minh Nguyen"));

        callApi();

        assertThat(displayNameOf(userId)).isEqualTo("Minh Nguyen");
    }

    @Test
    void profileNameFallsBackToEmailLocalPartWhenTokenHasNoDisplayName() throws Exception {
        UUID userId = UUID.randomUUID();
        tokenFor(userId, "minh@example.com", Map.of("email_verified", true));

        callApi();

        assertThat(displayNameOf(userId)).isEqualTo("minh");
    }

    @Test
    void profileNameFallsBackToEmailLocalPartWhenTokenHasNoUserMetadataAtAll() throws Exception {
        UUID userId = UUID.randomUUID();
        tokenFor(userId, "minh@example.com", null);

        callApi();

        assertThat(displayNameOf(userId)).isEqualTo("minh");
    }

    @Test
    void anonymousRequestCreatesNoProfile() throws Exception {
        long before = profileCount();

        mockMvc.perform(get("/api/v1/nothing-here")).andExpect(status().isUnauthorized());

        assertThat(profileCount()).isEqualTo(before);
    }

    @Test
    void requestWithInvalidTokenCreatesNoProfile() throws Exception {
        given(jwtDecoder.decode(TOKEN)).willThrow(new BadJwtException("Invalid signature"));
        long before = profileCount();

        mockMvc.perform(get("/api/v1/nothing-here").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isUnauthorized());

        assertThat(profileCount()).isEqualTo(before);
    }

    // Later steps assume the profile exists, so a failed upsert must stop the request, not be ignored.
    @Test
    void databaseFailureWhileCreatingProfileStopsTheRequest() {
        tokenFor(UUID.randomUUID(), "minh@example.com", Map.of());
        doThrow(new DataAccessResourceFailureException("database down")).when(userProfiles)
                .createIfAbsent(any(), any());

        assertThatThrownBy(this::callApi).isInstanceOf(DataAccessResourceFailureException.class);
    }

    private void tokenFor(UUID userId, String email, Map<String, Object> userMetadata) {
        Jwt.Builder jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "ES256")
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        if (userMetadata != null) {
            jwt.claim("user_metadata", userMetadata);
        }
        given(jwtDecoder.decode(TOKEN)).willReturn(jwt.build());
    }

    // No controller exists yet: 404 means the request passed security.
    private void callApi() throws Exception {
        mockMvc.perform(get("/api/v1/nothing-here").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .andExpect(status().isNotFound());
    }

    private String displayNameOf(UUID userId) {
        return jdbc.sql("SELECT display_name FROM app_users WHERE id = :id")
                .param("id", userId)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    private long profileCount() {
        return jdbc.sql("SELECT count(*) FROM app_users").query(Long.class).single();
    }

}
