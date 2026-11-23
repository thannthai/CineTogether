package com.thanh.cinetogether.catalog.domain.browse;

import java.util.List;

import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;

public record Home(List<MovieSummary> featured, List<Section> sections) {

    public record Section(String key, String title, List<MovieSummary> items) {
    }

}
