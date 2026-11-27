package com.thanh.cinetogether.catalog.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.browse.MovieList;
import com.thanh.cinetogether.catalog.domain.movie.Episode;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.PlaybackSource;
import com.thanh.cinetogether.catalog.domain.movie.StreamFormat;
import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;
import com.thanh.cinetogether.catalog.infrastructure.KKPhimResponses.Category;
import com.thanh.cinetogether.catalog.infrastructure.KKPhimResponses.Data;
import com.thanh.cinetogether.catalog.infrastructure.KKPhimResponses.DetailResponse;
import com.thanh.cinetogether.catalog.infrastructure.KKPhimResponses.EpisodeItem;
import com.thanh.cinetogether.catalog.infrastructure.KKPhimResponses.Item;
import com.thanh.cinetogether.catalog.infrastructure.KKPhimResponses.ListResponse;
import com.thanh.cinetogether.catalog.infrastructure.KKPhimResponses.Movie;
import com.thanh.cinetogether.catalog.infrastructure.KKPhimResponses.Server;

public class KKPhimProvider implements MovieProvider {

    private static final String KEY = "kkphim";

    private final RestClient client;

    public KKPhimProvider(String baseUrl) {
        this(baseUrl, Duration.ofSeconds(3), Duration.ofSeconds(5));
    }

    public KKPhimProvider(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        requestFactory.setReadTimeout(readTimeout);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public MoviePage browse(BrowseQuery query) {
        boolean searching = query.q() != null && !query.q().isBlank();
        ListResponse response;
        try {
            response = client.get()
                    .uri(uriBuilder -> listUri(uriBuilder, query, searching))
                    .retrieve()
                    .body(ListResponse.class);
        } catch (RestClientException e) {
            throw new ProviderUnavailableException("KKPhim could not be read.", e);
        }
        if (response == null || response.data() == null) {
            throw new ProviderUnavailableException("KKPhim sent a list without data.");
        }

        Data data = response.data();
        List<Item> items = data.items() == null ? List.of() : data.items();
        List<MovieSummary> movies = items.stream()
                .map(item -> new MovieSummary(KEY, item.slug(), item.name(), KKPhimMapping.blankToNull(item.originName()),
                        KKPhimMapping.movieType(item.type()),
                        KKPhimMapping.imageUrl(data.cdnImageDomain(), item.posterUrl()), item.year(),
                        item.category() == null ? List.of() : item.category().stream().map(Category::name).toList(),
                        KKPhimMapping.blankToNull(item.quality()), KKPhimMapping.blankToNull(item.lang()),
                        KKPhimMapping.progress(item.episodeCurrent(), null, null),
                        KKPhimMapping.ratingCount(item.imdb(), item.tmdb())))
                .toList();
        boolean hasNext = data.params() != null && data.params().pagination() != null
                && KKPhimMapping.hasNext(data.params().pagination().currentPage(),
                        data.params().pagination().totalPages());
        return new MoviePage(movies, query.paging().page(), query.paging().size(), hasNext);
    }

    @Override
    public Optional<MovieDetail> detail(String movieId) {
        DetailResponse response;
        try {
            response = client.get().uri("/phim/{slug}", movieId).retrieve().body(DetailResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ProviderUnavailableException("KKPhim could not be read.", e);
        }
        if (response == null || response.movie() == null) {
            return Optional.empty();
        }
        Movie movie = response.movie();

        // A movie can be on several servers that list the same episodes: keep each episode once, first server wins.
        Map<String, Episode> episodes = new LinkedHashMap<>();
        for (Server server : response.episodes() == null ? List.<Server>of() : response.episodes()) {
            for (EpisodeItem item : server.serverData() == null ? List.<EpisodeItem>of() : server.serverData()) {
                if (item.slug() != null) {
                    episodes.putIfAbsent(item.slug(),
                            new Episode(item.slug(), item.name() != null ? item.name() : item.slug()));
                }
            }
        }
        return Optional.of(new MovieDetail(KEY, movie.slug() != null ? movie.slug() : movieId, movie.name(),
                KKPhimMapping.blankToNull(movie.originName()), KKPhimMapping.movieType(movie.type()),
                KKPhimMapping.plainText(movie.content()), KKPhimMapping.imageUrl(null, movie.posterUrl()),
                KKPhimMapping.imageUrl(null, movie.thumbUrl()),
                movie.category() == null ? List.of() : movie.category().stream().map(Category::name).toList(),
                List.copyOf(episodes.values()), movie.year(), KKPhimMapping.blankToNull(movie.quality()),
                KKPhimMapping.blankToNull(movie.lang()), KKPhimMapping.blankToNull(movie.time()),
                KKPhimMapping.progress(movie.episodeCurrent(), movie.episodeTotal(), movie.status()), movie.view(),
                KKPhimMapping.names(movie.actor()), KKPhimMapping.names(movie.director()),
                KKPhimMapping.trailerUrl(movie.trailerUrl())));
    }

    @Override
    public Optional<PlaybackSource> playbackSource(String movieId, String episodeId) {
        DetailResponse response;
        try {
            response = client.get().uri("/phim/{slug}", movieId).retrieve().body(DetailResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ProviderUnavailableException("KKPhim could not be read.", e);
        }
        if (response == null || response.episodes() == null) {
            return Optional.empty();
        }
        // The first server that has an HLS link for this episode wins; the embed page links are not used.
        return response.episodes().stream()
                .filter(server -> server.serverData() != null)
                .flatMap(server -> server.serverData().stream())
                .filter(item -> episodeId.equals(item.slug()) && KKPhimMapping.isHlsUrl(item.linkM3u8()))
                .findFirst()
                .map(item -> new PlaybackSource(KEY, movieId, episodeId, item.linkM3u8(), StreamFormat.HLS));
    }

    // The provider has no type parameter: it filters by type through a different list, and its search cannot
    // filter by type at all.
    private URI listUri(UriBuilder uriBuilder, BrowseQuery query, boolean searching) {
        Map<String, String> variables = new HashMap<>();
        uriBuilder.path(searching ? "/v1/api/tim-kiem" : "/v1/api/danh-sach/" + listFor(query))
                .queryParam("page", KKPhimMapping.providerPage(query.paging()))
                .queryParam("limit", query.paging().size());
        // Values come from the user, so they go in as URI variables to be encoded.
        if (searching) {
            uriBuilder.queryParam("keyword", "{keyword}");
            variables.put("keyword", query.q());
        }
        if (query.genre() != null) {
            uriBuilder.queryParam("category", "{category}");
            variables.put("category", query.genre());
        }
        if (query.year() != null) {
            uriBuilder.queryParam("year", "{year}");
            variables.put("year", String.valueOf(query.year()));
        }
        if (!query.countries().slugs().isEmpty()) {
            uriBuilder.queryParam("country", "{country}");
            variables.put("country", String.join(",", query.countries().slugs()));
        }
        return uriBuilder.build(variables);
    }

    private static String listFor(BrowseQuery query) {
        if (query.list() == MovieList.CINEMA) {
            return "phim-chieu-rap";
        }
        if (query.list() == MovieList.ANIME) {
            return "hoat-hinh";
        }
        if (query.type() == MovieType.MOVIE) {
            return "phim-le";
        }
        return query.type() == MovieType.SERIES ? "phim-bo" : "phim-moi-cap-nhat";
    }

}
