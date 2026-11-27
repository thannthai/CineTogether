package com.thanh.cinetogether.catalog.domain.movie;

import java.util.List;

/** `ratingCount` is how many people rated the movie (IMDb, else TMDb); null when unknown. It tells how well known it is. */
public record MovieSummary(String providerKey, String movieId, String title, String originalTitle, MovieType type,
        String posterUrl, Integer releaseYear, List<String> genres, String quality, String language,
        EpisodeProgress progress, Integer ratingCount) {

    public MovieSummary(String providerKey, String movieId, String title, String originalTitle, MovieType type,
            String posterUrl, Integer releaseYear, List<String> genres, String quality, String language,
            EpisodeProgress progress) {
        this(providerKey, movieId, title, originalTitle, type, posterUrl, releaseYear, genres, quality, language,
                progress, null);
    }

    // A summary with only the basic fields; the rest is unknown.
    public MovieSummary(String providerKey, String movieId, String title, MovieType type, String posterUrl,
            Integer releaseYear, List<String> genres) {
        this(providerKey, movieId, title, null, type, posterUrl, releaseYear, genres, null, null,
                EpisodeProgress.NONE, null);
    }

}
