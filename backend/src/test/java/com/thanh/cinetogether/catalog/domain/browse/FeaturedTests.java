package com.thanh.cinetogether.catalog.domain.browse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.thanh.cinetogether.catalog.domain.movie.EpisodeProgress;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

class FeaturedTests {

    private static MovieSummary movie(String id, Integer year, Integer ratings) {
        return new MovieSummary("kkphim", id, id, null, MovieType.SERIES, "https://img.example/" + id + ".jpg", year,
                List.of(), null, null, EpisodeProgress.NONE, ratings);
    }

    private static MovieSummary withoutPoster(String id, Integer year, Integer ratings) {
        return new MovieSummary("kkphim", id, id, null, MovieType.SERIES, null, year, List.of(), null, null,
                EpisodeProgress.NONE, ratings);
    }

    private static List<String> ids(List<MovieSummary> movies) {
        return movies.stream().map(MovieSummary::movieId).toList();
    }

    @Test
    void theBestKnownMoviesOfThisYearComeFirst() {
        List<MovieSummary> movies = List.of(movie("a", 2026, 10), movie("b", 2026, 5000), movie("c", 2026, 90));

        assertThat(ids(Featured.pick(movies, 2026, 3))).containsExactly("b", "c", "a");
    }

    @Test
    void aMovieFromAnEarlierYearNeverCountsHoweverWellKnownItIs() {
        List<MovieSummary> movies = List.of(movie("old-hit", 2024, 1_000_000), movie("new", 2026, 5));

        assertThat(ids(Featured.pick(movies, 2026, 5))).containsExactly("new");
    }

    @Test
    void aMovieFromALaterYearCountsAsNew() {
        assertThat(ids(Featured.pick(List.of(movie("future", 2027, null)), 2026, 5))).containsExactly("future");
    }

    @Test
    void moviesWithEqualOrUnknownRatingsKeepTheProvidersOrderAndUnknownOnesComeLast() {
        List<MovieSummary> movies = List.of(movie("a", 2026, null), movie("b", 2026, null), movie("c", 2026, 50),
                movie("d", 2026, 50));

        assertThat(ids(Featured.pick(movies, 2026, 4))).containsExactly("c", "d", "a", "b");
    }

    @Test
    void aMovieWithoutAPosterIsNeverPicked() {
        List<MovieSummary> movies = List.of(withoutPoster("no-poster", 2026, 9999), movie("ok", 2026, 1));

        assertThat(ids(Featured.pick(movies, 2026, 5))).containsExactly("ok");
    }

    @Test
    void lastYearFillsTheMissingPlacesAfterThisYearsMovies() {
        List<MovieSummary> movies = List.of(movie("last-a", 2025, 100), movie("new", 2026, 1),
                movie("last-b", 2025, 900));

        assertThat(ids(Featured.pick(movies, 2026, 3))).containsExactly("new", "last-b", "last-a");
    }

    @Test
    void lastYearIsNotUsedWhenThisYearAlreadyFillsEveryPlace() {
        List<MovieSummary> movies = List.of(movie("last", 2025, 1_000_000), movie("a", 2026, 1), movie("b", 2026, 2));

        assertThat(ids(Featured.pick(movies, 2026, 2))).containsExactly("b", "a");
    }

    @Test
    void onlyThePreviousYearCanFillNotOlderOnes() {
        List<MovieSummary> movies = List.of(movie("two-years", 2024, 5), movie("last", 2025, 5));

        assertThat(ids(Featured.pick(movies, 2026, 5))).containsExactly("last");
    }

    @Test
    void keepsAtMostTheRequestedNumber() {
        List<MovieSummary> movies = List.of(movie("a", 2026, 1), movie("b", 2026, 2), movie("c", 2026, 3));

        assertThat(Featured.pick(movies, 2026, 2)).hasSize(2);
    }

    @Test
    void withoutAnyNewMovieThereIsNothingToFeature() {
        assertThat(Featured.pick(List.of(movie("old", 2020, 9), movie("none", null, 9)), 2026, 5)).isEmpty();
        assertThat(Featured.pick(List.of(), 2026, 5)).isEmpty();
    }

}
