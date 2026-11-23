package com.thanh.cinetogether.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thanh.cinetogether.TestcontainersConfiguration;
import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;

/** Who may call what in the catalog, with no token: one table, so a change to the security rules shows up here. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CatalogAccessRulesTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieProvider movieProvider;

    @BeforeEach
    void providerKnowsNoMovies() {
        given(movieProvider.key()).willReturn("kkphim");
        given(movieProvider.browse(any())).willReturn(new MoviePage(List.of(), 0, 20, false));
        given(movieProvider.detail("movie-one")).willReturn(Optional.empty());
    }

    // 404 and 200 mean the request got past security; 401 means it was stopped there.
    @ParameterizedTest(name = "{0} {1} without a token is {2}")
    @CsvSource({
            "GET,  /api/v1/catalog/home,                           200",
            "GET,  /api/v1/movies,                                 200",
            "GET,  /api/v1/movies/kkphim/movie-one,                404",
            "GET,  /api/v1/movies/kkphim/movie-one/playback?episodeId=tap-1, 401",
            "POST, /api/v1/movies,                                 401",
            "POST, /api/v1/catalog/home,                           401",
            "GET,  /api/v1/me,                                     401"
    })
    void anonymousAccessFollowsTheRules(String method, String uri, int expectedStatus) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), uri)).andExpect(status().is(expectedStatus));
    }

    @Test
    void preflightForTheCatalogIsAllowedFromTheFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/movies")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

}
