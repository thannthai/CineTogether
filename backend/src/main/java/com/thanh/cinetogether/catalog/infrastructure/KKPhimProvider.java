package com.thanh.cinetogether.catalog.infrastructure;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thanh.cinetogether.catalog.domain.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.MoviePage;
import com.thanh.cinetogether.catalog.domain.MovieProvider;
import com.thanh.cinetogether.catalog.domain.MovieSummary;
import com.thanh.cinetogether.catalog.domain.MovieType;

public class KKPhimProvider implements MovieProvider {

    private static final String KEY = "kkphim";

    private final RestClient client;

    public KKPhimProvider(String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public MoviePage browse(BrowseQuery query) {
        boolean searching = query.q() != null && !query.q().isBlank();
        ListResponse response = client.get()
                .uri(uriBuilder -> listUri(uriBuilder, query, searching))
                .retrieve()
                .body(ListResponse.class);

        Data data = response.data();
        List<Item> items = data.items() == null ? List.of() : data.items();
        List<MovieSummary> movies = items.stream()
                .map(item -> new MovieSummary(KEY, item.slug(), item.name(), KKPhimMapping.movieType(item.type()),
                        KKPhimMapping.imageUrl(data.cdnImageDomain(), item.posterUrl()), item.year(),
                        item.category() == null ? List.of() : item.category().stream().map(Category::name).toList()))
                .toList();
        boolean hasNext = data.params() != null && data.params().pagination() != null
                && KKPhimMapping.hasNext(data.params().pagination().currentPage(),
                        data.params().pagination().totalPages());
        return new MoviePage(movies, query.paging().page(), query.paging().size(), hasNext);
    }

    // The provider has no type parameter: it filters by type through a different list, and its search cannot
    // filter by type at all.
    private URI listUri(UriBuilder uriBuilder, BrowseQuery query, boolean searching) {
        Map<String, String> variables = new HashMap<>();
        uriBuilder.path(searching ? "/v1/api/tim-kiem" : "/v1/api/danh-sach/" + listFor(query.type()))
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
        if (query.country() != null) {
            uriBuilder.queryParam("country", "{country}");
            variables.put("country", query.country());
        }
        return uriBuilder.build(variables);
    }

    private static String listFor(MovieType type) {
        if (type == MovieType.MOVIE) {
            return "phim-le";
        }
        return type == MovieType.SERIES ? "phim-bo" : "phim-moi-cap-nhat";
    }

    record ListResponse(Data data) {
    }

    record Data(List<Item> items, Params params, @JsonProperty("APP_DOMAIN_CDN_IMAGE") String cdnImageDomain) {
    }

    record Params(Pagination pagination) {
    }

    record Pagination(int currentPage, int totalPages) {
    }

    record Item(String name, String slug, String type, @JsonProperty("poster_url") String posterUrl, Integer year,
            List<Category> category) {
    }

    record Category(String name) {
    }

}
