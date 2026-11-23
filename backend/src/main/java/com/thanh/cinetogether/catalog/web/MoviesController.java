package com.thanh.cinetogether.catalog.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thanh.cinetogether.catalog.application.Catalog;
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.browse.Paging;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

@RestController
@RequestMapping("/api/v1/movies")
class MoviesController {

    private final Catalog catalog;

    MoviesController(Catalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    MoviePage browse(@RequestParam(required = false) String q, @RequestParam(required = false) MovieType type,
            @RequestParam(required = false) String genre, @RequestParam(required = false) String country,
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        return catalog.browse(new BrowseQuery(q, type, genre, country, Paging.of(page, size)));
    }

}
