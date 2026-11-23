package com.thanh.cinetogether.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.IntStream;

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
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.browse.Paging;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class HomeEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieProvider movieProvider;

    private static MoviePage pageOf(int count) {
        List<MovieSummary> items = IntStream.rangeClosed(1, count)
                .mapToObj(i -> new MovieSummary("kkphim", "movie-" + i, "Movie " + i, MovieType.MOVIE, null, 2024,
                        List.of()))
                .toList();
        return new MoviePage(items, 0, 20, false);
    }

    @Test
    void homeIsPublicAndFeaturesTheFirstFiveOfTheNewUpdates() throws Exception {
        given(movieProvider.browse(any())).willReturn(pageOf(7));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.featured.length()").value(5))
                .andExpect(jsonPath("$.featured[0].movieId").value("movie-1"))
                .andExpect(jsonPath("$.featured[4].movieId").value("movie-5"))
                .andExpect(jsonPath("$.sections.length()").value(1))
                .andExpect(jsonPath("$.sections[0].key").value("new-updates"))
                .andExpect(jsonPath("$.sections[0].title").value("Mới cập nhật"))
                .andExpect(jsonPath("$.sections[0].items.length()").value(7))
                .andExpect(jsonPath("$.sections[0].items[6].movieId").value("movie-7"));
    }

    @Test
    void fewerThanFiveMoviesAreAllFeatured() throws Exception {
        given(movieProvider.browse(any())).willReturn(pageOf(2));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(2))
                .andExpect(jsonPath("$.sections[0].items.length()").value(2));
    }

    @Test
    void homeAsksTheProviderOnceForTheFirstPageWithoutFilters() throws Exception {
        given(movieProvider.browse(any())).willReturn(pageOf(0));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(0))
                .andExpect(jsonPath("$.sections[0].items.length()").value(0));

        verify(movieProvider).browse(new BrowseQuery(null, null, null, null, Paging.of(0, 20)));
    }

    @Test
    void anUnavailableProviderIsServiceUnavailable() throws Exception {
        given(movieProvider.browse(any())).willThrow(new ProviderUnavailableException("down"));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

}
