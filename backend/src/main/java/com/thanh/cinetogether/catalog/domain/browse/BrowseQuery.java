package com.thanh.cinetogether.catalog.domain.browse;

import com.thanh.cinetogether.catalog.domain.movie.MovieType;

/** Every filter is optional (null means "not filtered"). */
public record BrowseQuery(String q, MovieType type, String genre, CountryFilter countries, Paging paging) {

    public BrowseQuery {
        if (countries == null) {
            countries = CountryFilter.of(null);
        }
    }

}
