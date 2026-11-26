package com.thanh.cinetogether.catalog.application;

import java.time.Clock;
import java.time.Year;
import java.util.List;

import org.springframework.stereotype.Service;

import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.MovieNotFoundException;
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.browse.Featured;
import com.thanh.cinetogether.catalog.domain.browse.Home;
import com.thanh.cinetogether.catalog.domain.browse.Paging;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.PlaybackSource;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;

@Service
public class Catalog {

    private static final int FEATURED_COUNT = 5;

    private final MovieProvider provider;
    private final Clock clock;

    Catalog(MovieProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    public MoviePage browse(BrowseQuery query) {
        return provider.browse(query);
    }

    // Derived from one provider call: the provider has no "featured" list, so it is 5 of the new updates,
    // preferring movies released this year (see Featured).
    public Home home() {
        List<MovieSummary> newUpdates = provider.browse(new BrowseQuery(null, null, null, null, Paging.of(null, null)))
                .items();
        return new Home(Featured.pick(newUpdates, Year.now(clock).getValue(), FEATURED_COUNT),
                List.of(new Home.Section("new-updates", "Mới cập nhật", newUpdates)));
    }

    public MovieDetail detail(String providerKey, String movieId) {
        if (!provider.key().equals(providerKey)) {
            throw new MovieNotFoundException(providerKey, movieId);
        }
        return provider.detail(movieId).orElseThrow(() -> new MovieNotFoundException(providerKey, movieId));
    }

    public PlaybackSource playback(String providerKey, String movieId, String episodeId) {
        if (!provider.key().equals(providerKey)) {
            throw new MovieNotFoundException(providerKey, movieId);
        }
        return provider.playbackSource(movieId, episodeId)
                .orElseThrow(() -> new MovieNotFoundException(providerKey, movieId));
    }

}
