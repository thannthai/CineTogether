package com.thanh.cinetogether.catalog.application;

import org.springframework.stereotype.Service;

import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
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

}
