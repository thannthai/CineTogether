package com.thanh.cinetogether.config;

import java.io.IOException;
import java.net.URI;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

/**
 * Answers a missing or invalid token with 401 and the {@code application/problem+json} body from
 * {@code docs/design/api.md} §1.2.
 * <p>
 * Spring's own entry point still builds the {@code WWW-Authenticate: Bearer ...} challenge (including
 * {@code error="invalid_token"} for a bad token), because OAuth2 clients rely on it; this class only adds the body. The
 * body is the same whatever the reason, so it does not reveal why a token was rejected.
 */
class ProblemJsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final AuthenticationEntryPoint bearerChallenge = new BearerTokenAuthenticationEntryPoint();

    private final JsonMapper jsonMapper;

    ProblemJsonAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        bearerChallenge.commence(request, response, authException);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Authentication is required or the access token is invalid.");
        // Spring leaves the type null (omitted from the JSON), which RFC 9457 reads as about:blank; api.md shows it
        // explicitly, so set it.
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", "UNAUTHENTICATED");

        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }

}
