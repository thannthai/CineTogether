package com.thanh.cinetogether.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;

import com.thanh.cinetogether.TestcontainersConfiguration;

/**
 * Real HTTP against the embedded server (random port), complementing the MockMvc tests in
 * {@code BearerOnlySecurityTests}. MockMvc never runs the servlet container, so only a real request shows the actual
 * status and the actual {@code Set-Cookie} headers a browser or curl would receive.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnonymousHttpRequestTests {

    @LocalServerPort
    private int port;

    // No CookieHandler is configured, so the client never stores or replays cookies; it only exposes the headers.
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * An anonymous request, for any method, gets 401 with a Bearer challenge and no {@code Set-Cookie} header.
     * <p>
     * A {@code JSESSIONID} cookie would show that the server created an HTTP session for a caller that never logged
     * in. The API is stateless and authenticates only through the bearer token, so no cookie of any kind may be set.
     * {@code Accept: *}{@code /*} is what curl sends by default and the case in which the cookie was first observed.
     */
    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void anonymousRequestGetsUnauthorizedWithoutAnyCookie(String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/nothing-here"))
                .header(HttpHeaders.ACCEPT, "*/*")
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue(HttpHeaders.WWW_AUTHENTICATE)).hasValueSatisfying(
                challenge -> assertThat(challenge).startsWith("Bearer"));
        assertThat(response.headers().allValues(HttpHeaders.SET_COOKIE)).isEmpty();
    }

}
