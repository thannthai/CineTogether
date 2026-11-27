package com.thanh.cinetogether.catalog.domain.browse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

class FeaturedTests {

    private static MovieSummary movie(String id, Integer year) {
        return new MovieSummary("kkphim", id, id, MovieType.SERIES, null, year, List.of());
    }

    private static List<String> ids(List<MovieSummary> movies) {
        return movies.stream().map(MovieSummary::movieId).toList();
    }

    @Test
    void theMostViewedMoviesOfThisYearComeFirst() {
        List<MovieSummary> updates = List.of(movie("a", 2026), movie("b", 2026), movie("c", 2026));

        assertThat(ids(Featured.pick(updates, Map.of("a", 10, "b", 500, "c", 90), 2026, 3)))
                .containsExactly("b", "c", "a");
    }

    @Test
    void aMovieFromAnEarlierYearNeverCountsHoweverMuchItIsViewed() {
        List<MovieSummary> updates = List.of(movie("old-hit", 2024), movie("new", 2026));

        assertThat(ids(Featured.pick(updates, Map.of("old-hit", 1_000_000, "new", 5), 2026, 5)))
                .containsExactly("new");
    }

    @Test
    void aMovieFromALaterYearCountsAsNew() {
        assertThat(ids(Featured.pick(List.of(movie("future", 2027)), Map.of(), 2026, 5))).containsExactly("future");
    }

    @Test
    void moviesWithEqualOrUnknownViewsKeepTheProvidersOrderAndUnknownOnesComeLast() {
        List<MovieSummary> updates = List.of(movie("a", 2026), movie("b", 2026), movie("c", 2026), movie("d", 2026));

        assertThat(ids(Featured.pick(updates, Map.of("c", 50, "d", 50), 2026, 4)))
                .containsExactly("c", "d", "a", "b");
    }

    @Test
    void lastYearFillsTheMissingPlacesAfterThisYearsMovies() {
        List<MovieSummary> updates = List.of(movie("last-a", 2025), movie("new", 2026), movie("last-b", 2025));

        assertThat(ids(Featured.pick(updates, Map.of("new", 1, "last-a", 100, "last-b", 900), 2026, 3)))
                .containsExactly("new", "last-b", "last-a");
    }

    @Test
    void lastYearIsNotUsedWhenThisYearAlreadyFillsEveryPlace() {
        List<MovieSummary> updates = List.of(movie("last", 2025), movie("a", 2026), movie("b", 2026));

        assertThat(ids(Featured.pick(updates, Map.of("last", 1_000_000), 2026, 2))).containsExactly("a", "b");
    }

    @Test
    void onlyThePreviousYearCanFillNotOlderOnes() {
        List<MovieSummary> updates = List.of(movie("two-years", 2024), movie("last", 2025));

        assertThat(ids(Featured.pick(updates, Map.of(), 2026, 5))).containsExactly("last");
    }

    @Test
    void keepsAtMostTheRequestedNumber() {
        List<MovieSummary> updates = List.of(movie("a", 2026), movie("b", 2026), movie("c", 2026));

        assertThat(Featured.pick(updates, Map.of(), 2026, 2)).hasSize(2);
    }

    @Test
    void withoutAnyNewMovieThereIsNothingToFeature() {
        assertThat(Featured.pick(List.of(movie("old", 2020), movie("none", null)), Map.of("old", 9), 2026, 5)).isEmpty();
        assertThat(Featured.pick(List.of(), Map.of(), 2026, 5)).isEmpty();
    }

    @Test
    void aMovieWithoutAYearIsNotNew() {
        assertThat(ids(Featured.pick(List.of(movie("unknown", null), movie("new", 2026)), Map.of(), 2026, 5)))
                .containsExactly("new");
    }

}
