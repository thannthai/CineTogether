package com.thanh.cinetogether.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thanh.cinetogether.identity.ProfileProvisioner;

/**
 * The API authenticates with a bearer token only (no cookies, no server session). The real {@code Authorization}
 * header goes through the real bearer-token filter; only the {@link JwtDecoder} is faked, so no Supabase call is made.
 * There is no controller yet, so a request that passes security ends in 404.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class BearerOnlySecurityTests {

    private static final String VALID_TOKEN = "valid-token";
    private static final String BAD_TOKEN = "bad-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ProfileProvisioner profileProvisioner;

    private void stubDecoder() {
        Jwt jwt = Jwt.withTokenValue(VALID_TOKEN)
                .header("alg", "ES256")
                .subject("00000000-0000-0000-0000-000000000001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        given(jwtDecoder.decode(VALID_TOKEN)).willReturn(jwt);
        given(jwtDecoder.decode(BAD_TOKEN)).willThrow(new BadJwtException("Invalid signature"));
    }

    /**
     * B4: a request with a valid bearer token gets past Spring Security for every HTTP method.
     * <p>
     * No controller exists yet, so the expected status is 404: the request cleared the security filters and reached
     * MVC, which found no handler. A 401 would mean the bearer token was not accepted, and a 403 would mean
     * something such as CSRF protection blocked an authenticated state-changing request. Both are what this test
     * guards against when the security configuration is changed.
     */
    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PATCH", "PUT", "DELETE"})
    void validBearerTokenPassesSecurityForEveryHttpMethod(String method) throws Exception {
        stubDecoder();

        mockMvc.perform(request(HttpMethod.valueOf(method), "/api/v1/nothing-here")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound());
    }

    /**
     * B1: an anonymous request that is rejected with 401 must not create an HTTP session.
     * <p>
     * With the default session policy, the filter that handles the rejection saves the unauthenticated request in
     * the {@code HttpSession} so it could be replayed after a form login. That creates a {@code JSESSIONID} cookie
     * on a response that has no login at all. {@code Accept: *}{@code /*} is what curl sends by default, and it is
     * the case where this was observed.
     */
    @Test
    void anonymousRequestIsRejectedWithBearerChallengeAndWithoutCreatingSession() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/nothing-here")
                        .header(HttpHeaders.ACCEPT, "*/*"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(result -> assertNull(
                        result.getRequest().getSession(false), "no session must be created"));
    }

    /**
     * B2: anonymous state-changing requests are rejected with 401 and must not create an HTTP session either.
     * <p>
     * These methods take a different path through the filter chain than GET (the CSRF filter runs before
     * authorization), so B1 does not prove they are session-free. A session here would hand a {@code JSESSIONID}
     * cookie to an unauthenticated caller.
     */
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void anonymousStateChangingRequestIsRejectedWithoutCreatingSession(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), "/api/v1/nothing-here")
                        .header(HttpHeaders.ACCEPT, "*/*"))
                .andExpect(status().isUnauthorized())
                .andExpect(result -> assertNull(
                        result.getRequest().getSession(false), "no session must be created"));
    }

    /**
     * B3: a request with an invalid bearer token is rejected with 401 {@code invalid_token} and no session is created.
     * <p>
     * {@code getSession(false)} returns the existing session or {@code null} without creating one. It must be
     * {@code null} because a session means a {@code JSESSIONID} cookie, and this API is stateless: identity comes
     * only from the bearer token on each request. A session would also be server state that a client could mistake
     * for a login.
     */
    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST"})
    void invalidBearerTokenIsRejectedWithoutCreatingSession(String method) throws Exception {
        stubDecoder();

        mockMvc.perform(request(HttpMethod.valueOf(method), "/api/v1/nothing-here")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + BAD_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")))
                .andExpect(result -> assertNull(
                        result.getRequest().getSession(false), "no session must be created"));
    }

}
