package com.thanh.cinetogether.catalog.domain.movie;

public record PlaybackSource(String providerKey, String movieId, String episodeId, String streamUrl,
        StreamFormat format) {
}
