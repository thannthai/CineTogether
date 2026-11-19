package com.thanh.cinetogether.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * REST authentication failures use the error format of {@code docs/design/api.md} §1.2: an
 * {@code application/problem+json} body with a stable machine-readable {@code code}, so the frontend can map it to a
 * user-facing message without parsing text. The standard {@code WWW-Authenticate: Bearer} challenge must stay.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class ProblemJsonErrorTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    /**
     * A request without any token gets 401 with a problem+json body and code {@code UNAUTHENTICATED}.
     * <p>
     * The Bearer challenge stays because OAuth2 clients rely on it, and no cookie is set (the API is stateless).
     */
    @Test
    void anonymousRequestGetsUnauthenticatedProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/nothing-here"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    /**
     * An invalid token gets the same problem+json body as a missing one, so the response does not reveal why the token
     * was rejected. The reason ({@code invalid_token}) is still in the {@code WWW-Authenticate} header, as OAuth2
     * defines.
     */
    @Test
    void invalidBearerTokenGetsUnauthenticatedProblemJson() throws Exception {
        given(jwtDecoder.decode("bad-token")).willThrow(new BadJwtException("Invalid signature"));

        mockMvc.perform(get("/api/v1/nothing-here").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

}
