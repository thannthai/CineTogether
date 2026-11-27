package com.thanh.cinetogether.catalog.domain.browse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

/** The five movies of the home hero: a mix, so the hero is not all one kind. */
public final class HeroPicks {

    public static final int COUNT = 5;
    static final int CINEMA_FILMS = 2;
    static final int KOREAN_SERIES = 1;
    static final int OTHER_SERIES = 2;

    private HeroPicks() {
    }

    // The best known cinema films, the best known Korean series and the best known other series (anime included)
    // of this year (see Featured for "best known" and for how last year fills a short list). A movie is picked once. A
    // slot nobody can fill is taken by the best known movie left in any of the lists. The most rated one comes first, after
    // which last year's movies come.
    public static List<MovieSummary> pick(List<MovieSummary> cinema, List<MovieSummary> koreanSeries,
            List<MovieSummary> movies, int currentYear) {
        Map<String, MovieSummary> picked = new LinkedHashMap<>();
        add(picked, Featured.pick(cinema, currentYear, CINEMA_FILMS));
        add(picked, Featured.pick(koreanSeries, currentYear, KOREAN_SERIES));
        add(picked, Featured.pick(notPicked(series(movies), picked), currentYear, OTHER_SERIES));

        if (picked.size() < COUNT) {
            List<MovieSummary> everything = new ArrayList<>(cinema);
            everything.addAll(koreanSeries);
            everything.addAll(movies);
            add(picked, Featured.pick(notPicked(everything, picked), currentYear, COUNT - picked.size()));
        }
        // This year's movies stay ahead of last year's fill; inside each group the most rated comes first.
        return picked.values().stream()
                .sorted(Comparator
                        .comparing((MovieSummary movie) -> movie.releaseYear() != null && movie.releaseYear() >= currentYear)
                        .thenComparing(movie -> movie.ratingCount() == null ? -1 : movie.ratingCount())
                        .reversed())
                .toList();
    }

    private static void add(Map<String, MovieSummary> picked, List<MovieSummary> movies) {
        movies.forEach(movie -> picked.putIfAbsent(movie.movieId(), movie));
    }

    private static List<MovieSummary> series(List<MovieSummary> movies) {
        return movies.stream().filter(movie -> movie.type() == MovieType.SERIES).toList();
    }

    private static List<MovieSummary> notPicked(List<MovieSummary> movies, Map<String, MovieSummary> picked) {
        return movies.stream().filter(movie -> !picked.containsKey(movie.movieId())).toList();
    }

}
