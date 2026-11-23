package com.thanh.cinetogether.catalog.domain;

/** No such movie: the provider key is not ours, or the provider does not know the movie. */
public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(String providerKey, String movieId) {
        super("Movie not found: " + providerKey + "/" + movieId);
    }

}
