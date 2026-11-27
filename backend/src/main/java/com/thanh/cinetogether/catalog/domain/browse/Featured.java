package com.thanh.cinetogether.catalog.domain.browse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;

public final class Featured {

    private Featured() {
    }

    // This year's movies (or later) come first, ranked by views, most first. Only when fewer than `count` exist do
    // movies of the previous year fill the missing places, also ranked by views; older ones never count. A movie whose
    // views are unknown goes after the known ones; ties keep the provider's order (the sort is stable).
    public static List<MovieSummary> pick(List<MovieSummary> newUpdates, Map<String, Integer> views, int currentYear,
            int count) {
        List<MovieSummary> picked = new ArrayList<>(
                ranked(newUpdates, views, movie -> movie.releaseYear() != null && movie.releaseYear() >= currentYear));
        if (picked.size() < count) {
            picked.addAll(ranked(newUpdates, views,
                    movie -> movie.releaseYear() != null && movie.releaseYear() == currentYear - 1));
        }
        return picked.stream().limit(count).toList();
    }

    private static List<MovieSummary> ranked(List<MovieSummary> movies, Map<String, Integer> views,
            java.util.function.Predicate<MovieSummary> filter) {
        return movies.stream()
                .filter(filter)
                .sorted(Comparator.comparing((MovieSummary movie) -> views.getOrDefault(movie.movieId(), -1)).reversed())
                .toList();
    }

}
