package com.thanh.cinetogether.catalog.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Year;
import java.time.ZoneOffset;
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
import com.thanh.cinetogether.catalog.domain.browse.CountryFilter;
import com.thanh.cinetogether.catalog.domain.browse.MovieList;
import com.thanh.cinetogether.catalog.domain.browse.Paging;
import com.thanh.cinetogether.catalog.domain.movie.EpisodeProgress;
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

    private static final BrowseQuery NEW_UPDATES = new BrowseQuery(null, null, null, null, Paging.of(0, 20));
    private static final BrowseQuery THIS_YEAR_POOL = new BrowseQuery(null, null, null, null, null, THIS_YEAR,
            Paging.of(0, 50));
    private static final BrowseQuery LAST_YEAR_POOL = new BrowseQuery(null, null, null, null, null, THIS_YEAR - 1,
            Paging.of(0, 50));
    private static final BrowseQuery CINEMA = new BrowseQuery(null, null, null, null, MovieList.CINEMA,
            Paging.of(0, 50));

    private static final BrowseQuery KOREAN = new BrowseQuery(null, MovieType.SERIES, null,
            CountryFilter.of(List.of("han-quoc")), null, THIS_YEAR, Paging.of(0, 50));

    private static final MoviePage NONE = page();

    private static MoviePage page(MovieSummary... movies) {
        return new MoviePage(List.of(movies), 0, 50, false);
    }

    private static MovieSummary film(String movieId, int year, Integer ratings) {
        return new MovieSummary("kkphim", movieId, movieId, null, MovieType.MOVIE,
                "https://img.example/" + movieId + ".jpg", year, List.of(), null, null, EpisodeProgress.NONE, ratings);
    }

    private static MovieSummary series(String movieId, int year, Integer ratings) {
        return new MovieSummary("kkphim", movieId, movieId, null, MovieType.SERIES,
                "https://img.example/" + movieId + ".jpg", year, List.of(), null, null, EpisodeProgress.NONE, ratings);
    }

    private static MoviePage updates(int count) {
        List<MovieSummary> items = IntStream.rangeClosed(1, count)
                .mapToObj(i -> new MovieSummary("kkphim", "movie-" + i, "Movie " + i, MovieType.MOVIE, null, THIS_YEAR,
                        List.of()))
                .toList();
        return new MoviePage(items, 0, 20, false);
    }

    // The four lists the home page reads; the ones not given are empty.
    private void provider(MoviePage newUpdates, MoviePage thisYear, MoviePage lastYear, MoviePage cinema) {
        given(movieProvider.browse(KOREAN)).willReturn(NONE);
        given(movieProvider.browse(NEW_UPDATES)).willReturn(newUpdates);
        given(movieProvider.browse(THIS_YEAR_POOL)).willReturn(thisYear);
        given(movieProvider.browse(LAST_YEAR_POOL)).willReturn(lastYear);
        given(movieProvider.browse(CINEMA)).willReturn(cinema);
    }

    @Test
    void homeIsPublicAndListsTheNewUpdatesInTheProvidersOrder() throws Exception {
        provider(updates(7), NONE, NONE, NONE);

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sections.length()").value(1))
                .andExpect(jsonPath("$.sections[0].key").value("new-updates"))
                .andExpect(jsonPath("$.sections[0].title").value("Mới cập nhật"))
                .andExpect(jsonPath("$.sections[0].items.length()").value(7))
                .andExpect(jsonPath("$.sections[0].items[6].movieId").value("movie-7"));
    }

    @Test
    void featuredAreThisYearsBestKnownMoviesAndNeverOlderOnes() throws Exception {
        provider(updates(1), page(film("old-hit", THIS_YEAR - 6, 999_999), film("new-1", THIS_YEAR, 100),
                film("new-2", THIS_YEAR, 5000), film("new-3", THIS_YEAR, null)), NONE, NONE);

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(3))
                .andExpect(jsonPath("$.featured[0].movieId").value("new-2"))
                .andExpect(jsonPath("$.featured[1].movieId").value("new-1"))
                .andExpect(jsonPath("$.featured[2].movieId").value("new-3"));
    }

    @Test
    void theHeroMixesCinemaFilmsKoreanSeriesAndOtherSeries() throws Exception {
        provider(updates(1), page(series("s-1", THIS_YEAR, 5000), series("s-2", THIS_YEAR, 3000),
                series("s-3", THIS_YEAR, 10)), NONE,
                page(film("spider", THIS_YEAR, 183_000), film("toy", THIS_YEAR, 62_000),
                        film("third", THIS_YEAR, 39_000)));
        given(movieProvider.browse(KOREAN))
                .willReturn(page(series("k-1", THIS_YEAR, 9000), series("k-2", THIS_YEAR, 100)));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(5))
                .andExpect(jsonPath("$.featured[0].movieId").value("spider"))
                .andExpect(jsonPath("$.featured[1].movieId").value("toy"))
                .andExpect(jsonPath("$.featured[2].movieId").value("k-1"))
                .andExpect(jsonPath("$.featured[3].movieId").value("s-1"))
                .andExpect(jsonPath("$.featured[4].movieId").value("s-2"));
    }

    @Test
    void lastYearFillsTheHeroOnlyWhenThisYearHasTooFew() throws Exception {
        provider(updates(1), page(film("new", THIS_YEAR, 1)),
                page(film("last-a", THIS_YEAR - 1, 100), film("last-b", THIS_YEAR - 1, 900)), NONE);

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(3))
                .andExpect(jsonPath("$.featured[0].movieId").value("new"))
                .andExpect(jsonPath("$.featured[1].movieId").value("last-b"))
                .andExpect(jsonPath("$.featured[2].movieId").value("last-a"));
    }

    @Test
    void lastYearsListIsNotReadWhenThisYearFillsTheHero() throws Exception {
        provider(updates(1), page(film("a", THIS_YEAR, 1), film("b", THIS_YEAR, 2), film("c", THIS_YEAR, 3),
                film("d", THIS_YEAR, 4), film("e", THIS_YEAR, 5), film("f", THIS_YEAR, 6)), NONE, NONE);

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(5))
                .andExpect(jsonPath("$.featured[0].movieId").value("f"));

        verify(movieProvider, never()).browse(LAST_YEAR_POOL);
    }

    @Test
    void aFailingHeroListDoesNotBreakTheHomePage() throws Exception {
        provider(updates(2), NONE, NONE, NONE);
        given(movieProvider.browse(THIS_YEAR_POOL)).willThrow(new ProviderUnavailableException("down"));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featured.length()").value(0))
                .andExpect(jsonPath("$.sections[0].items.length()").value(2));
    }

    @Test
    void theCinemaSectionHoldsThisYearsBestKnownCinemaFilms() throws Exception {
        provider(NONE, NONE, NONE, page(film("old", THIS_YEAR - 2, 99_999), film("c-1", THIS_YEAR, 10),
                film("c-2", THIS_YEAR, 900), film("c-3", THIS_YEAR, null)));

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
        provider(NONE, NONE, NONE, page(film("last-1", THIS_YEAR - 1, 5), film("new", THIS_YEAR, 1),
                film("last-2", THIS_YEAR - 1, 700), film("old", THIS_YEAR - 2, 50)));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[1].items.length()").value(3))
                .andExpect(jsonPath("$.sections[1].items[0].movieId").value("new"))
                .andExpect(jsonPath("$.sections[1].items[1].movieId").value("last-2"))
                .andExpect(jsonPath("$.sections[1].items[2].movieId").value("last-1"));
    }

    @Test
    void aFailingCinemaListDoesNotBreakTheHomePage() throws Exception {
        provider(updates(2), NONE, NONE, NONE);
        given(movieProvider.browse(CINEMA)).willThrow(new ProviderUnavailableException("down"));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.length()").value(1))
                .andExpect(jsonPath("$.sections[0].key").value("new-updates"));
    }

    @Test
    void withoutCinemaFilmsOfThisYearTheCinemaSectionIsLeftOut() throws Exception {
        provider(updates(2), NONE, NONE, page(film("old", THIS_YEAR - 2, 5)));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections.length()").value(1));
    }

    @Test
    void anUnavailableListOfNewUpdatesIsServiceUnavailable() throws Exception {
        given(movieProvider.browse(NEW_UPDATES)).willThrow(new ProviderUnavailableException("down"));

        mockMvc.perform(get("/api/v1/catalog/home"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"));
    }

}
