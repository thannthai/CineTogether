package com.thanh.cinetogether.catalog.domain;

import java.util.Optional;

import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;

public interface MovieProvider {

    String key();

    MoviePage browse(BrowseQuery query);

    /** Empty when the provider does not know the movie. */
    Optional<MovieDetail> detail(String movieId);

}
