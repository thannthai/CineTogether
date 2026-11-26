package com.thanh.cinetogether.catalog.domain.browse;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;

public final class Featured {

    private Featured() {
    }

    // Only movies released this year (or later) can be featured, however much an older one is viewed: they are ranked
    // by views, most first. A movie whose views are unknown goes after the known ones; ties keep the provider's order
    // (the sort is stable). When fewer than `count` are new, fewer are returned.
    public static List<MovieSummary> pick(List<MovieSummary> newUpdates, Map<String, Integer> views, int currentYear,
            int count) {
        return newUpdates.stream()
                .filter(movie -> movie.releaseYear() != null && movie.releaseYear() >= currentYear)
                .sorted(Comparator.comparing((MovieSummary movie) -> views.getOrDefault(movie.movieId(), -1))
                        .reversed())
                .limit(count)
                .toList();
    }

}
