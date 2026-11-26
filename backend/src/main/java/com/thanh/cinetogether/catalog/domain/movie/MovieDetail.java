package com.thanh.cinetogether.catalog.domain.movie;

import java.util.List;

public record MovieDetail(String providerKey, String movieId, String title, MovieType type, String synopsis,
        String posterUrl, String backdropUrl, List<String> genres, List<Episode> episodes) {
}
