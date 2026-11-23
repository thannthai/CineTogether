package com.thanh.cinetogether.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.thanh.cinetogether.TestcontainersConfiguration;
import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.browse.Paging;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MoviesEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieProvider movieProvider;

    @Test
    void browsingIsPublicAndReturnsThePageAsJson() throws Exception {
        given(movieProvider.browse(any())).willReturn(new MoviePage(List.of(new MovieSummary("kkphim", "movie-one",
                "Movie One", MovieType.MOVIE, "https://img.example/one.jpg", 2024, List.of("Action"))), 0, 20, true));

        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.items[0].providerKey").value("kkphim"))
                .andExpect(jsonPath("$.items[0].movieId").value("movie-one"))
                .andExpect(jsonPath("$.items[0].title").value("Movie One"))
                .andExpect(jsonPath("$.items[0].type").value("MOVIE"))
                .andExpect(jsonPath("$.items[0].posterUrl").value("https://img.example/one.jpg"))
                .andExpect(jsonPath("$.items[0].releaseYear").value(2024))
                .andExpect(jsonPath("$.items[0].genres[0]").value("Action"));
    }

    @Test
    void queryParametersReachTheProvider() throws Exception {
        given(movieProvider.browse(any())).willReturn(new MoviePage(List.of(), 2, 10, false));

        mockMvc.perform(get("/api/v1/movies?q=love&type=MOVIE&genre=action&country=korea&page=2&size=10"))
                .andExpect(status().isOk());

        verify(movieProvider).browse(new BrowseQuery("love", MovieType.MOVIE, "action", "korea", Paging.of(2, 10)));
    }

    @Test
    void missingParametersUseTheDefaults() throws Exception {
        given(movieProvider.browse(any())).willReturn(new MoviePage(List.of(), 0, 20, false));

        mockMvc.perform(get("/api/v1/movies")).andExpect(status().isOk());

        verify(movieProvider).browse(new BrowseQuery(null, null, null, null, Paging.of(0, 20)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"size=51", "size=0", "page=-1", "type=BOOK"})
    void aBadParameterIsRejectedWithValidationError(String parameter) throws Exception {
        mockMvc.perform(get("/api/v1/movies?" + parameter))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void anUnavailableProviderIsServiceUnavailable() throws Exception {
        given(movieProvider.browse(any())).willThrow(new ProviderUnavailableException("down"));

        ResultActions result = mockMvc.perform(get("/api/v1/movies"));

        result.andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

    @Test
    void theCauseOfAnUnavailableProviderIsLoggedButNotReturned(CapturedOutput output) throws Exception {
        given(movieProvider.browse(any())).willThrow(
                new ProviderUnavailableException("KKPhim could not be read.", new RuntimeException("connection reset")));

        String body = mockMvc.perform(get("/api/v1/movies")).andExpect(status().isServiceUnavailable())
                .andReturn().getResponse().getContentAsString();

        assertThat(output).contains("connection reset");
        assertThat(body).doesNotContain("connection reset");
    }

}
