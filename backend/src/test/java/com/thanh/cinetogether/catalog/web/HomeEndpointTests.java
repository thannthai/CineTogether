package com.thanh.cinetogether.catalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
import com.thanh.cinetogether.catalog.domain.browse.MovieList;
import com.thanh.cinetogether.catalog.domain.browse.Paging;
import com.thanh.cinetogether.catalog.domain.movie.EpisodeProgress;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
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

    private static final int THIS_YEAR = Year.now(ZoneOffset.UTC).getValue();

    private static MoviePage pageOf(int count) {
        List<MovieSummary> items = IntStream.rangeClosed(1, count)
                .mapToObj(i -> new MovieSummary("kkphim", "movie-" + i, "Movie " + i, MovieType.MOVIE, null, THIS_YEAR,
                        List.of()))
                .toList();
        return new MoviePage(items, 0, 20, false);
    }

    private static MovieDetail detailWithViews(String movieId, int views) {
        return new MovieDetail("kkphim", movieId, movieId, null, MovieType.SERIES, null, null, null, List.of(),
                List.of(), THIS_YEAR, null, null, null, EpisodeProgress.NONE, views, List.of(), List.of(), null);
    }

    @Test
    void featuredAreThisYearsMostViewedMoviesAndNeverOlderOnes() throws Exception {
        MovieType type = MovieType.SERIES;
        List<MovieSummary> items = List.of(
                new MovieSummary("kkphim", "old-hit", "Old", type, null, THIS_YEAR - 6, List.of()),
                new MovieSummary("kkphim", "new-1", "New 1", type, null, THIS_YEAR, List.of()),
                new MovieSummary("kkphim", "new-2", "New 2", MovieType.MOVIE, null, THIS_YEAR, List.of()),
                new MovieSummary("kkphim", "new-3", "New 3", type, null, THIS_YEAR, List.of()));
        given(movieProvider.browse(any())).willReturn(new MoviePage(items, 0, 20, false));
        given(movieProvider.detail("new-1")).willReturn(Optional.of(detailWithViews("new-1", 10)));
        given(movieProvider.detail("new-2")).willReturn(Optional.of(detailWithViews("new-2", 900)));
        // new-3's views are unknown (the provider has no detail), so it ranks after the known ones.

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(3))
                .andExpect(jsonPath("$.featured[0].movieId").value("new-2"))
                .andExpect(jsonPath("$.featured[1].movieId").value("new-1"))
                .andExpect(jsonPath("$.featured[2].movieId").value("new-3"))
                .andExpect(jsonPath("$.sections[0].items[0].movieId").value("old-hit"));
    }

    private static final BrowseQuery NEW_UPDATES = new BrowseQuery(null, null, null, null, Paging.of(0, 20));
    private static final BrowseQuery CINEMA = new BrowseQuery(null, null, null, null, MovieList.CINEMA,
            Paging.of(0, 50));

    private static MovieSummary cinemaFilm(String movieId, int year) {
        return new MovieSummary("kkphim", movieId, movieId, MovieType.MOVIE, null, year, List.of());
    }

    @Test
    void theCinemaSectionHoldsThisYearsMostViewedCinemaFilms() throws Exception {
        given(movieProvider.browse(NEW_UPDATES)).willReturn(pageOf(0));
        given(movieProvider.browse(CINEMA)).willReturn(new MoviePage(List.of(cinemaFilm("old", THIS_YEAR - 2),
                cinemaFilm("c-1", THIS_YEAR), cinemaFilm("c-2", THIS_YEAR), cinemaFilm("c-3", THIS_YEAR)), 0, 50,
                false));
        given(movieProvider.detail("c-1")).willReturn(Optional.of(detailWithViews("c-1", 10)));
        given(movieProvider.detail("c-2")).willReturn(Optional.of(detailWithViews("c-2", 900)));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[1].key").value("cinema"))
                .andExpect(jsonPath("$.sections[1].title").value("Phim chiếu rạp"))
                .andExpect(jsonPath("$.sections[1].items.length()").value(3))
                .andExpect(jsonPath("$.sections[1].items[0].movieId").value("c-2"))
                .andExpect(jsonPath("$.sections[1].items[1].movieId").value("c-1"))
                .andExpect(jsonPath("$.sections[1].items[2].movieId").value("c-3"));
    }

    @Test
    void lastYearsCinemaFilmsFillTheSectionWhenThisYearHasTooFew() throws Exception {
        given(movieProvider.browse(NEW_UPDATES)).willReturn(pageOf(0));
        given(movieProvider.browse(CINEMA)).willReturn(new MoviePage(List.of(cinemaFilm("last-1", THIS_YEAR - 1),
                cinemaFilm("new", THIS_YEAR), cinemaFilm("last-2", THIS_YEAR - 1), cinemaFilm("old", THIS_YEAR - 2)),
                0, 50, false));
        given(movieProvider.detail("last-2")).willReturn(Optional.of(detailWithViews("last-2", 700)));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[1].items.length()").value(3))
                .andExpect(jsonPath("$.sections[1].items[0].movieId").value("new"))
                .andExpect(jsonPath("$.sections[1].items[1].movieId").value("last-2"))
                .andExpect(jsonPath("$.sections[1].items[2].movieId").value("last-1"));
    }

    @Test
    void aFailingCinemaListDoesNotBreakTheHomePage() throws Exception {
        given(movieProvider.browse(NEW_UPDATES)).willReturn(pageOf(2));
        given(movieProvider.browse(CINEMA)).willThrow(new ProviderUnavailableException("down"));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.length()").value(1))
                .andExpect(jsonPath("$.sections[0].key").value("new-updates"));
    }

    @Test
    void withoutCinemaFilmsOfThisYearTheCinemaSectionIsLeftOut() throws Exception {
        given(movieProvider.browse(NEW_UPDATES)).willReturn(pageOf(2));
        given(movieProvider.browse(CINEMA)).willReturn(new MoviePage(List.of(cinemaFilm("old", THIS_YEAR - 2)), 0, 50,
                false));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.length()").value(1));
    }

    @Test
    void aFailingDetailDoesNotBreakTheHomePage() throws Exception {
        given(movieProvider.browse(any())).willReturn(pageOf(3));
        given(movieProvider.detail("movie-2")).willThrow(new ProviderUnavailableException("slow"));
        given(movieProvider.detail("movie-3")).willReturn(Optional.of(detailWithViews("movie-3", 50)));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(3))
                .andExpect(jsonPath("$.featured[0].movieId").value("movie-3"));
    }

    @Test
    void withoutAnyMovieOfThisYearNothingIsFeaturedButTheUpdatesAreStillListed() throws Exception {
        given(movieProvider.browse(any())).willReturn(new MoviePage(
                List.of(new MovieSummary("kkphim", "old", "Old", MovieType.MOVIE, null, THIS_YEAR - 3, List.of())), 0,
                20, false));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(0))
                .andExpect(jsonPath("$.sections[0].items.length()").value(1));
    }

    @Test
    void homeIsPublicAndFeaturesFiveOfTheNewUpdatesInTheirOrderWhenViewsAreUnknown() throws Exception {
        given(movieProvider.browse(NEW_UPDATES)).willReturn(pageOf(7));
        given(movieProvider.browse(CINEMA)).willReturn(pageOf(0));

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
