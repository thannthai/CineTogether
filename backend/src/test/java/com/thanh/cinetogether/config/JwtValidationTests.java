package com.thanh.cinetogether.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Token validation with the real {@code JwtDecoder} that Spring Boot builds from {@code application.yaml}.
 * <p>
 * Unlike {@code BearerOnlySecurityTests}, nothing is mocked: the tests sign real ES256 tokens with a freshly generated
 * key and serve its public half as a JWKS from a local HTTP server, which replaces Supabase's JWKS endpoint. A token
 * that passes validation ends in 404 (no controller exists yet); a rejected token ends in 401.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class JwtValidationTests {

    private static final String ISSUER = "https://test-project.invalid/auth/v1";
    private static final String KEY_ID = "test-key";
    private static final ECKey SIGNING_KEY = generateKey(KEY_ID);
    private static final HttpServer JWKS_SERVER = startJwksServer(SIGNING_KEY);

    @DynamicPropertySource
    static void resourceServerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost:" + JWKS_SERVER.getAddress().getPort() + "/jwks");
    }

    @AfterAll
    static void stopJwksServer() {
        JWKS_SERVER.stop(0);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tokenWithValidSignatureIssuerAudienceAndExpiryIsAccepted() throws Exception {
        String token = token(SIGNING_KEY, ISSUER, "authenticated", Instant.now().plusSeconds(300));

        mockMvc.perform(get("/api/v1/nothing-here").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    /**
     * A token that is correctly signed and issued by the right issuer, but meant for a different audience, must be
     * rejected.
     * <p>
     * Spring Boot checks {@code aud} only when {@code audiences} is configured. Without it this token is accepted,
     * so any token the same Supabase project signs for another purpose would work against this API.
     */
    @Test
    void tokenForAnotherAudienceIsRejected() throws Exception {
        String token = token(SIGNING_KEY, ISSUER, "some-other-service", Instant.now().plusSeconds(300));

        assertRejected(token);
    }

    /** A token without any {@code aud} claim cannot be shown to be meant for this API, so it is rejected too. */
    @Test
    void tokenWithoutAudienceIsRejected() throws Exception {
        String token = token(SIGNING_KEY, ISSUER, null, Instant.now().plusSeconds(300));

        assertRejected(token);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() throws Exception {
        String token = token(SIGNING_KEY, "https://other-project.invalid/auth/v1", "authenticated",
                Instant.now().plusSeconds(300));

        assertRejected(token);
    }

    /** The expiry is five minutes in the past, well beyond the 60 seconds of clock skew Spring tolerates. */
    @Test
    void expiredTokenIsRejected() throws Exception {
        String token = token(SIGNING_KEY, ISSUER, "authenticated", Instant.now().minusSeconds(300));

        assertRejected(token);
    }

    /**
     * A token that claims the published {@code kid} but was signed with a different private key must be rejected:
     * the signature check is what stops anyone but Supabase from minting tokens.
     */
    @Test
    void tokenSignedWithAnotherKeyIsRejected() throws Exception {
        ECKey attackerKey = generateKey(KEY_ID);
        String token = token(attackerKey, ISSUER, "authenticated", Instant.now().plusSeconds(300));

        assertRejected(token);
    }

    private void assertRejected(String token) throws Exception {
        mockMvc.perform(get("/api/v1/nothing-here").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
    }

    private static String token(ECKey key, String issuer, String audience, Instant expiresAt) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("00000000-0000-0000-0000-000000000001")
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiresAt));
        if (audience != null) {
            claims.audience(audience);
        }
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).type(JOSEObjectType.JWT).build(),
                claims.build());
        try {
            jwt.sign(new ECDSASigner(key));
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return jwt.serialize();
    }

    private static ECKey generateKey(String keyId) {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID(keyId).keyUse(KeyUse.SIGNATURE).algorithm(JWSAlgorithm.ES256)
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Serves only the public half of the key, as Supabase's JWKS endpoint does. */
    private static HttpServer startJwksServer(ECKey key) {
        byte[] jwks = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/jwks", exchange -> {
                exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
                exchange.sendResponseHeaders(200, jwks.length);
                exchange.getResponseBody().write(jwks);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
