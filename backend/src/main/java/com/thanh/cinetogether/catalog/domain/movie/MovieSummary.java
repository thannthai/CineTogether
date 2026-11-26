package com.thanh.cinetogether.catalog.domain.movie;

import java.util.List;

public record MovieSummary(String providerKey, String movieId, String title, String originalTitle, MovieType type,
        String posterUrl, Integer releaseYear, List<String> genres, String quality, String language,
        EpisodeProgress progress) {

    // A summary with only the basic fields; the rest is unknown.
    public MovieSummary(String providerKey, String movieId, String title, MovieType type, String posterUrl,
            Integer releaseYear, List<String> genres) {
        this(providerKey, movieId, title, null, type, posterUrl, releaseYear, genres, null, null,
                EpisodeProgress.NONE);
    }

}
