package com.thanh.cinetogether.catalog.application;

import org.springframework.stereotype.Service;

import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.MovieNotFoundException;
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;

@Service
public class Catalog {

    private final MovieProvider provider;

    Catalog(MovieProvider provider) {
        this.provider = provider;
    }

    public MoviePage browse(BrowseQuery query) {
        return provider.browse(query);
    }

    public MovieDetail detail(String providerKey, String movieId) {
        if (!provider.key().equals(providerKey)) {
            throw new MovieNotFoundException(providerKey, movieId);
        }
        return provider.detail(movieId).orElseThrow(() -> new MovieNotFoundException(providerKey, movieId));
    }

}
