package com.thanh.cinetogether.catalog.domain.browse;

import com.thanh.cinetogether.catalog.domain.movie.MovieType;

/** Every filter is optional (null means "not filtered"). */
public record BrowseQuery(String q, MovieType type, String genre, CountryFilter countries, MovieList list,
        Integer year, Paging paging) {

    public static final int MIN_YEAR = 1900;
    public static final int MAX_YEAR = 2100;

    public BrowseQuery {
        if (countries == null) {
            countries = CountryFilter.of(null);
        }
        if (year != null && (year < MIN_YEAR || year > MAX_YEAR)) {
            throw new InvalidFilterException("year must be between " + MIN_YEAR + " and " + MAX_YEAR + ".");
        }
    }

    public BrowseQuery(String q, MovieType type, String genre, CountryFilter countries, MovieList list,
            Paging paging) {
        this(q, type, genre, countries, list, null, paging);
    }

    public BrowseQuery(String q, MovieType type, String genre, CountryFilter countries, Paging paging) {
        this(q, type, genre, countries, null, null, paging);
    }

}
