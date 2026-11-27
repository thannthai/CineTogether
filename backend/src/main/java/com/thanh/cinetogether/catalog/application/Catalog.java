package com.thanh.cinetogether.catalog.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.MovieNotFoundException;
import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.browse.Featured;
import com.thanh.cinetogether.catalog.domain.browse.Home;
import com.thanh.cinetogether.catalog.domain.browse.MovieList;
import com.thanh.cinetogether.catalog.domain.browse.Paging;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.PlaybackSource;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;

@Service
public class Catalog {

    private static final Logger log = LoggerFactory.getLogger(Catalog.class);

    private static final int FEATURED_COUNT = 5;
    private static final int CINEMA_COUNT = 14;
    // One page of the cinema list is enough to hold well over a dozen films of the current year.
    private static final Paging CINEMA_PAGE = Paging.of(0, 50);
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
    // viewed ones, and the views come from the details of the first few candidates (see Featured). The cinema
    // section ranks this year's cinema films the same way, and it is left out when that list fails.
    public Home home() {
        List<MovieSummary> newUpdates = provider.browse(new BrowseQuery(null, null, null, null, Paging.of(null, null)))
                .items();
        List<MovieSummary> cinema = cinemaFilms();
        int year = Year.now(clock).getValue();

        // One batch of detail calls for both lists, so the 4 second limit is paid once. A movie in both is read once.
        Map<String, MovieSummary> candidates = new LinkedHashMap<>();
        viewCandidates(newUpdates, year, FEATURED_COUNT).forEach(movie -> candidates.putIfAbsent(movie.movieId(), movie));
        viewCandidates(cinema, year, CINEMA_COUNT).forEach(movie -> candidates.putIfAbsent(movie.movieId(), movie));
        Map<String, Integer> views = viewsOf(List.copyOf(candidates.values()));

        List<Home.Section> sections = new ArrayList<>();
        sections.add(new Home.Section("new-updates", "Mới cập nhật", newUpdates));
        List<MovieSummary> cinemaPicks = Featured.pick(cinema, views, year, CINEMA_COUNT);
        if (!cinemaPicks.isEmpty()) {
            sections.add(new Home.Section("cinema", "Phim chiếu rạp", cinemaPicks));
        }
        return new Home(Featured.pick(newUpdates, views, year, FEATURED_COUNT), sections);
    }

    // The first few movies of this year; when there are fewer than `wanted` of them, a few of last year's too, since
    // those can fill the missing places (see Featured).
    private static List<MovieSummary> viewCandidates(List<MovieSummary> movies, int year, int wanted) {
        List<MovieSummary> candidates = new ArrayList<>(movies.stream()
                .filter(movie -> movie.releaseYear() != null && movie.releaseYear() >= year)
                .limit(VIEW_CANDIDATES)
                .toList());
        if (candidates.size() < wanted) {
            movies.stream()
                    .filter(movie -> movie.releaseYear() != null && movie.releaseYear() == year - 1)
                    .limit(VIEW_CANDIDATES - candidates.size())
                    .forEach(candidates::add);
        }
        return candidates;
    }

    private List<MovieSummary> cinemaFilms() {
        try {
            return provider.browse(new BrowseQuery(null, null, null, null, MovieList.CINEMA, CINEMA_PAGE)).items();
        } catch (ProviderUnavailableException e) {
            log.warn("The cinema list is unavailable, so the home page has no cinema section", e);
            return List.of();
        }
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
