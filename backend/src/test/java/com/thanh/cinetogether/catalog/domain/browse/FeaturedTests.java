package com.thanh.cinetogether.catalog.domain.browse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
    void moviesReleasedThisYearComeFirstInTheProvidersOrder() {
        List<MovieSummary> updates = List.of(movie("old-a", 2020), movie("new-a", 2026), movie("old-b", 2023),
                movie("new-b", 2026), movie("new-c", 2026));

        assertThat(ids(Featured.pick(updates, 2026, 3))).containsExactly("new-a", "new-b", "new-c");
    }

    @Test
    void aMovieFromALaterYearCountsAsNew() {
        List<MovieSummary> updates = List.of(movie("old", 2020), movie("future", 2027));

        assertThat(ids(Featured.pick(updates, 2026, 1))).containsExactly("future");
    }

    @Test
    void whenTooFewAreNewTheRestFillInInTheProvidersOrder() {
        List<MovieSummary> updates = List.of(movie("old-a", 2020), movie("new-a", 2026), movie("old-b", 2023),
                movie("old-c", 2019));

        assertThat(ids(Featured.pick(updates, 2026, 3))).containsExactly("new-a", "old-a", "old-b");
    }

    @Test
    void withoutAnyNewMovieItIsTheFirstOnesInOrder() {
        List<MovieSummary> updates = List.of(movie("a", 2020), movie("b", 2021), movie("c", 2022));

        assertThat(ids(Featured.pick(updates, 2026, 2))).containsExactly("a", "b");
    }

    @Test
    void aMovieWithoutAYearIsNotCountedAsNew() {
        List<MovieSummary> updates = List.of(movie("unknown", null), movie("new", 2026));

        assertThat(ids(Featured.pick(updates, 2026, 1))).containsExactly("new");
        assertThat(ids(Featured.pick(updates, 2026, 2))).containsExactly("new", "unknown");
    }

    @Test
    void neverReturnsMoreThanRequestedOrMoreThanThereAre() {
        assertThat(Featured.pick(List.of(), 2026, 5)).isEmpty();
        assertThat(Featured.pick(List.of(movie("a", 2026)), 2026, 5)).hasSize(1);
    }
}
