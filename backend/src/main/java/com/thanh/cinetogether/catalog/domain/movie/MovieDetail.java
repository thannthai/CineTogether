package com.thanh.cinetogether.catalog.domain.movie;

import java.util.List;

public record MovieDetail(String providerKey, String movieId, String title, String originalTitle, MovieType type,
        String synopsis, String posterUrl, String backdropUrl, List<String> genres, List<Episode> episodes,
        Integer releaseYear, String quality, String language, String duration, EpisodeProgress progress,
        Integer views, List<String> actors, List<String> directors, String trailerUrl) {

    // A detail with only the basic fields; the rest is unknown.
    public MovieDetail(String providerKey, String movieId, String title, MovieType type, String synopsis,
            String posterUrl, String backdropUrl, List<String> genres, List<Episode> episodes) {
        this(providerKey, movieId, title, null, type, synopsis, posterUrl, backdropUrl, genres, episodes, null, null,
                null, null, EpisodeProgress.NONE, null, List.of(), List.of(), null);
    }

}
