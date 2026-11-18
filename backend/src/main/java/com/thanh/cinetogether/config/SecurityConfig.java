package com.thanh.cinetogether.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Baseline HTTP security for a stateless, bearer-token-only API: every request needs a valid Supabase JWT in the
 * {@code Authorization} header, plus CORS so the browser frontend can call the API. Per-endpoint rules (public catalog
 * and public rooms) are added with the features that need them.
 * <p>
 * Stateless means the server creates no {@code HttpSession} and sets no cookie, not even for rejected anonymous
 * requests. {@code STATELESS} stops the rejected request from being saved in a session, and CSRF protection is
 * disabled because its default token repository creates a session on state-changing requests; it is also pointless
 * here, since the browser never attaches a bearer token on its own. Both settings must be revisited together if
 * cookie-based authentication is ever introduced. {@code BearerOnlySecurityTests} and
 * {@code AnonymousHttpRequestTests} guard this behaviour.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // See the class Javadoc: re-enable CSRF if cookie-based authentication (for example a BFF) is added.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Allows the configured frontend origins to call {@code /api/**} with a bearer token. No cookies are used, so
     * credentials are not allowed.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

}
