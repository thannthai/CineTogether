package com.thanh.cinetogether.catalog.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.thanh.cinetogether.TestcontainersConfiguration;
import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.movie.PlaybackSource;
import com.thanh.cinetogether.catalog.domain.movie.StreamFormat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlaybackEndpointTests {

    private static final String PLAYBACK = "/api/v1/movies/kkphim/movie-one/playback?episodeId=tap-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieProvider movieProvider;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void providerIsKKPhimAndTheTokenIsValid() {
        given(movieProvider.key()).willReturn("kkphim");
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "ES256")
                .subject(UUID.randomUUID().toString())
                .claim("email", "user@example.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        given(jwtDecoder.decode("token")).willReturn(jwt);
    }

    @Test
    void aSignedInUserGetsTheStreamSource() throws Exception {
        given(movieProvider.playbackSource("movie-one", "tap-1")).willReturn(Optional.of(new PlaybackSource("kkphim",
                "movie-one", "tap-1", "https://cdn.example/one/tap-1/index.m3u8", StreamFormat.HLS)));

        playback(PLAYBACK)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.providerKey").value("kkphim"))
                .andExpect(jsonPath("$.movieId").value("movie-one"))
                .andExpect(jsonPath("$.episodeId").value("tap-1"))
                .andExpect(jsonPath("$.streamUrl").value("https://cdn.example/one/tap-1/index.m3u8"))
                .andExpect(jsonPath("$.format").value("HLS"));
    }

    @Test
    void anAnonymousCallerIsUnauthenticatedAndTheProviderIsNotAsked() throws Exception {
        mockMvc.perform(get(PLAYBACK))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        verify(movieProvider, never()).playbackSource("movie-one", "tap-1");
    }

    @Test
    void noPlayableSourceIsNotFound() throws Exception {
        given(movieProvider.playbackSource("movie-one", "tap-1")).willReturn(Optional.empty());

        playback(PLAYBACK)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MOVIE_NOT_FOUND"));
    }

    @Test
    void aDifferentProviderKeyIsNotFoundWithoutAskingTheProvider() throws Exception {
        playback("/api/v1/movies/other/movie-one/playback?episodeId=tap-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MOVIE_NOT_FOUND"));

        verify(movieProvider, never()).playbackSource("movie-one", "tap-1");
    }

    @Test
    void aMissingEpisodeIdIsAValidationError() throws Exception {
        playback("/api/v1/movies/kkphim/movie-one/playback")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void anUnavailableProviderIsServiceUnavailable() throws Exception {
        given(movieProvider.playbackSource("movie-one", "tap-1")).willThrow(new ProviderUnavailableException("down"));

        playback(PLAYBACK)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

    private ResultActions playback(String uri) throws Exception {
        return mockMvc.perform(get(uri).header(HttpHeaders.AUTHORIZATION, "Bearer token"));
    }

}
