package com.thanh.cinetogether.catalog.domain.browse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;

public final class Featured {

    private Featured() {
    }

    // Movies of this year (or later) come first, the most rated first: how many people rated a movie tells how well
    // known it is. Only when fewer than `count` exist do movies of the previous year fill the missing places, ranked
    // the same way; older ones never count. A movie without a poster is never picked. A movie with an unknown rating
    // count goes after the known ones; ties keep the provider's order (the sort is stable).
    public static List<MovieSummary> pick(List<MovieSummary> movies, int currentYear, int count) {
        List<MovieSummary> candidates = movies.stream()
                .filter(movie -> movie.posterUrl() != null && !movie.posterUrl().isBlank())
                .toList();
        List<MovieSummary> picked = new ArrayList<>(
                ranked(candidates, movie -> movie.releaseYear() != null && movie.releaseYear() >= currentYear));
        if (picked.size() < count) {
            picked.addAll(ranked(candidates,
                    movie -> movie.releaseYear() != null && movie.releaseYear() == currentYear - 1));
        }
        return picked.stream().limit(count).toList();
    }

    private static List<MovieSummary> ranked(List<MovieSummary> movies, Predicate<MovieSummary> filter) {
        return movies.stream()
                .filter(filter)
                .sorted(Comparator.comparing((MovieSummary movie) -> movie.ratingCount() == null ? -1 : movie.ratingCount())
                        .reversed())
                .toList();
    }

}
