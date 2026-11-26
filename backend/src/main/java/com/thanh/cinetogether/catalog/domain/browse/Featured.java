package com.thanh.cinetogether.catalog.domain.browse;

import java.util.ArrayList;
import java.util.List;

import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;

public final class Featured {

    private Featured() {
    }

    // Movies released this year (or later) come first, in the provider's order; the rest fill in when there
    // are too few. The provider orders by last update, so an old series with a new episode is often first.
    public static List<MovieSummary> pick(List<MovieSummary> newUpdates, int currentYear, int count) {
        List<MovieSummary> picked = new ArrayList<>();
        newUpdates.stream().filter(movie -> isNew(movie, currentYear)).limit(count).forEach(picked::add);
        newUpdates.stream().filter(movie -> !isNew(movie, currentYear)).limit(count - picked.size())
                .forEach(picked::add);
        return List.copyOf(picked);
    }

    private static boolean isNew(MovieSummary movie, int currentYear) {
        return movie.releaseYear() != null && movie.releaseYear() >= currentYear;
    }

}
