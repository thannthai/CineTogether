package com.thanh.cinetogether.catalog.domain;

import java.util.Optional;

import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.PlaybackSource;

public interface MovieProvider {

    String key();

    MoviePage browse(BrowseQuery query);

    /** Empty when the provider does not know the movie. */
    Optional<MovieDetail> detail(String movieId);

    /** Empty when the movie or episode is unknown, or the episode has no HLS source. */
    Optional<PlaybackSource> playbackSource(String movieId, String episodeId);

}
