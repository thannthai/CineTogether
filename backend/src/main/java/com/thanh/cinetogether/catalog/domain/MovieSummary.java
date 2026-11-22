package com.thanh.cinetogether.catalog.domain;

import java.util.List;

public record MovieSummary(String providerKey, String movieId, String title, MovieType type, String posterUrl,
        Integer releaseYear, List<String> genres) {
}
