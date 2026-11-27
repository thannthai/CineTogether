package com.thanh.cinetogether.catalog.application;

import java.time.Clock;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.MovieNotFoundException;
import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.browse.CountryFilter;
import com.thanh.cinetogether.catalog.domain.browse.Featured;
import com.thanh.cinetogether.catalog.domain.browse.HeroPicks;
import com.thanh.cinetogether.catalog.domain.browse.Home;
import com.thanh.cinetogether.catalog.domain.browse.MovieList;
import com.thanh.cinetogether.catalog.domain.browse.Paging;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.PlaybackSource;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;

@Service
public class Catalog {

    private static final Logger log = LoggerFactory.getLogger(Catalog.class);

    private static final String KOREA = "han-quoc";
    private static final int CINEMA_COUNT = 14;
    // One page of a list holds well over a dozen films of the current year.
    private static final Paging POOL_PAGE = Paging.of(0, 50);

    private final MovieProvider provider;
    private final Clock clock;

    Catalog(MovieProvider provider, Clock clock) {
        this.provider = provider;
        this.clock = clock;
    }

    public MoviePage browse(BrowseQuery query) {
        return provider.browse(query);
    }

    // Derived from the provider. `new-updates` is one call. The featured movies are a mix of this year's best known
    // cinema films, Korean series and other series (see HeroPicks), taken from one page of each (a page of last year's
    // movies only when this year has too few); the cinema section ranks the cinema list the same way (see Featured). Both are left out when their lists fail:
    // only the list of new updates is essential. The list items carry how many people rated each movie, so no
    // call per movie is needed.
    public Home home() {
        List<MovieSummary> newUpdates = provider.browse(new BrowseQuery(null, null, null, null, Paging.of(null, null)))
                .items();
        int year = Year.now(clock).getValue();

        List<MovieSummary> cinemaPool = optionalList(
                new BrowseQuery(null, null, null, null, MovieList.CINEMA, POOL_PAGE), "cinema");
        List<MovieSummary> koreanPool = optionalList(new BrowseQuery(null, MovieType.SERIES, null,
                CountryFilter.of(List.of(KOREA)), null, year, POOL_PAGE), "Korean series");
        List<MovieSummary> thisYear = yearPool(year);
        List<MovieSummary> featured = HeroPicks.pick(cinemaPool, koreanPool, thisYear, year);
        if (featured.size() < HeroPicks.COUNT) {
            featured = HeroPicks.pick(cinemaPool, koreanPool, concat(thisYear, yearPool(year - 1)), year);
        }

        List<Home.Section> sections = new ArrayList<>();
        sections.add(new Home.Section("new-updates", "Mới cập nhật", newUpdates));
        List<MovieSummary> cinema = Featured.pick(cinemaPool, year, CINEMA_COUNT);
        if (!cinema.isEmpty()) {
            sections.add(new Home.Section("cinema", "Phim chiếu rạp", cinema));
        }
        return new Home(featured, sections);
    }

    private List<MovieSummary> yearPool(int year) {
        return optionalList(new BrowseQuery(null, null, null, null, null, year, POOL_PAGE), "featured");
    }

    private static List<MovieSummary> concat(List<MovieSummary> first, List<MovieSummary> second) {
        List<MovieSummary> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private List<MovieSummary> optionalList(BrowseQuery query, String what) {
        try {
            return provider.browse(query).items();
        } catch (ProviderUnavailableException e) {
            log.warn("The {} list is unavailable, so the home page goes without it", what, e);
            return List.of();
        }
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
