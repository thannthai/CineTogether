package com.thanh.cinetogether.catalog.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    // The list has no view counts, only the detail of each movie does: look at this many of this year's movies.
    private static final int VIEW_CANDIDATES = 12;
    private static final Duration VIEWS_TIMEOUT = Duration.ofSeconds(4);

    private final MovieProvider provider;
    private final Clock clock;

    Catalog(MovieProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    public MoviePage browse(BrowseQuery query) {
        return provider.browse(query);
    }

    // Derived from the provider: the list of new updates is one call; the featured movies are this year's most
    // viewed ones, and the views come from the details of the first few candidates (see Featured).
    public Home home() {
        List<MovieSummary> newUpdates = provider.browse(new BrowseQuery(null, null, null, null, Paging.of(null, null)))
                .items();
        int year = Year.now(clock).getValue();
        List<MovieSummary> candidates = newUpdates.stream()
                .filter(movie -> movie.releaseYear() != null && movie.releaseYear() >= year)
                .limit(VIEW_CANDIDATES)
                .toList();
        return new Home(Featured.pick(newUpdates, viewsOf(candidates), year, FEATURED_COUNT),
                List.of(new Home.Section("new-updates", "Mới cập nhật", newUpdates)));
    }

    // The views of each movie, read from its detail in parallel. A detail that fails or comes too late is left out:
    // that movie just ranks after the known ones.
    private Map<String, Integer> viewsOf(List<MovieSummary> movies) {
        Map<String, Integer> views = new HashMap<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<String, Future<Optional<MovieDetail>>> pending = new HashMap<>();
            for (MovieSummary movie : movies) {
                pending.put(movie.movieId(), executor.submit(() -> provider.detail(movie.movieId())));
            }
            long deadline = System.nanoTime() + VIEWS_TIMEOUT.toNanos();
            pending.forEach((movieId, future) -> {
                try {
                    long left = Math.max(0, deadline - System.nanoTime());
                    future.get(left, TimeUnit.NANOSECONDS).map(MovieDetail::views)
                            .ifPresent(count -> views.put(movieId, count));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | TimeoutException e) {
                    future.cancel(true);
                }
            });
        }
        return views;
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
