package com.thanh.cinetogether.catalog.domain.browse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.thanh.cinetogether.catalog.domain.movie.EpisodeProgress;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

class HeroPicksTests {

    private static MovieSummary movie(String id, MovieType type, Integer year, Integer ratings) {
        return new MovieSummary("kkphim", id, id, null, type, "https://img.example/" + id + ".jpg", year, List.of(),
                null, null, EpisodeProgress.NONE, ratings);
    }

    private static MovieSummary film(String id, Integer ratings) {
        return movie(id, MovieType.MOVIE, 2026, ratings);
    }

    private static MovieSummary series(String id, Integer ratings) {
        return movie(id, MovieType.SERIES, 2026, ratings);
    }

    private static List<String> ids(List<MovieSummary> movies) {
        return movies.stream().map(MovieSummary::movieId).toList();
    }

    @Test
    void twoCinemaFilmsOneKoreanSeriesAndTwoOtherSeriesMostRatedFirst() {
        List<MovieSummary> cinema = List.of(film("spider", 183_000), film("toy", 62_000), film("third", 39_000));
        List<MovieSummary> korean = List.of(series("k-1", 9000), series("k-2", 100));
        List<MovieSummary> movies = List.of(series("s-1", 5000), series("s-2", 3000), series("s-3", 10));

        assertThat(ids(HeroPicks.pick(cinema, korean, movies, 2026)))
                .containsExactly("spider", "toy", "k-1", "s-1", "s-2");
    }

    @Test
    void aMovieInTwoListsIsPickedOnce() {
        List<MovieSummary> cinema = List.of(film("spider", 183_000));
        List<MovieSummary> korean = List.of(series("k-1", 9000));
        List<MovieSummary> movies = List.of(film("spider", 183_000), series("k-1", 9000), series("s-1", 5000));

        assertThat(ids(HeroPicks.pick(cinema, korean, movies, 2026))).containsExactly("spider", "k-1", "s-1");
    }

    @Test
    void otherSeriesSlotsIgnoreFilms() {
        List<MovieSummary> movies = List.of(film("hit", 900_000), series("s-1", 5));

        // The film fills a place only because nothing else is left.
        assertThat(ids(HeroPicks.pick(List.of(), List.of(), movies, 2026))).containsExactly("hit", "s-1");
    }

    @Test
    void aSlotNobodyCanFillGoesToTheBestKnownMovieLeft() {
        List<MovieSummary> cinema = List.of(film("c-1", 900), film("c-2", 800), film("c-3", 700), film("c-4", 600));
        List<MovieSummary> movies = List.of(series("s-1", 500), series("s-2", 400));

        // No Korean series: the fifth place is taken by the next best cinema film.
        assertThat(ids(HeroPicks.pick(cinema, List.of(), movies, 2026)))
                .containsExactly("c-1", "c-2", "c-3", "s-1", "s-2");
    }

    @Test
    void returnsWhatThereIsWhenThereAreFewerThanFiveMovies() {
        assertThat(ids(HeroPicks.pick(List.of(film("only", 5)), List.of(), List.of(), 2026))).containsExactly("only");
        assertThat(HeroPicks.pick(List.of(), List.of(), List.of(), 2026)).isEmpty();
    }

    @Test
    void lastYearsFillComesAfterThisYearsMoviesWhateverItsRating() {
        List<MovieSummary> movies = List.of(movie("last", MovieType.SERIES, 2025, 900_000), series("new", 5));

        assertThat(ids(HeroPicks.pick(List.of(), List.of(), movies, 2026))).containsExactly("new", "last");
    }

    @Test
    void oldMoviesAreNeverPicked() {
        List<MovieSummary> cinema = List.of(movie("old", MovieType.MOVIE, 2020, 999_999));

        assertThat(HeroPicks.pick(cinema, List.of(), List.of(), 2026)).isEmpty();
    }

}
