package com.thanh.cinetogether.catalog.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thanh.cinetogether.TestcontainersConfiguration;
import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.movie.Episode;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MovieDetailEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieProvider movieProvider;

    @BeforeEach
    void providerIsKKPhim() {
        given(movieProvider.key()).willReturn("kkphim");
    }

    @Test
    void detailIsPublicAndReturnsTheMovieWithItsEpisodes() throws Exception {
        given(movieProvider.detail("movie-one")).willReturn(Optional.of(new MovieDetail("kkphim", "movie-one",
                "Movie One", MovieType.SERIES, "A synopsis.", "https://img.example/one.jpg",
                "https://img.example/one-thumb.jpg", List.of("Action"),
                List.of(new Episode("tap-1", "Tap 1"), new Episode("tap-2", "Tap 2")))));

        mockMvc.perform(get("/api/v1/movies/kkphim/movie-one"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.providerKey").value("kkphim"))
                .andExpect(jsonPath("$.movieId").value("movie-one"))
                .andExpect(jsonPath("$.title").value("Movie One"))
                .andExpect(jsonPath("$.type").value("SERIES"))
                .andExpect(jsonPath("$.synopsis").value("A synopsis."))
                .andExpect(jsonPath("$.posterUrl").value("https://img.example/one.jpg"))
                .andExpect(jsonPath("$.backdropUrl").value("https://img.example/one-thumb.jpg"))
                .andExpect(jsonPath("$.genres[0]").value("Action"))
                .andExpect(jsonPath("$.episodes.length()").value(2))
                .andExpect(jsonPath("$.episodes[0].episodeId").value("tap-1"))
                .andExpect(jsonPath("$.episodes[1].title").value("Tap 2"));
    }

    @Test
    void anUnknownMovieIsNotFound() throws Exception {
        given(movieProvider.detail("missing")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/movies/kkphim/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("MOVIE_NOT_FOUND"));
    }

    @Test
    void aDifferentProviderKeyIsNotFoundWithoutAskingTheProvider() throws Exception {
        mockMvc.perform(get("/api/v1/movies/other/movie-one"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MOVIE_NOT_FOUND"));

        verify(movieProvider, never()).detail("movie-one");
    }

    @Test
    void anUnavailableProviderIsServiceUnavailable() throws Exception {
        given(movieProvider.detail("movie-one")).willThrow(new ProviderUnavailableException("down"));

        mockMvc.perform(get("/api/v1/movies/kkphim/movie-one"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

}
