package com.thanh.cinetogether.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thanh.cinetogether.catalog.domain.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.MoviePage;
import com.thanh.cinetogether.catalog.domain.MovieSummary;
import com.thanh.cinetogether.catalog.domain.MovieType;
import com.thanh.cinetogether.catalog.domain.Paging;

class KKPhimProviderTests {

    private static final String LIST_PATH = "/v1/api/danh-sach/phim-moi-cap-nhat";
    private static final String SEARCH_PATH = "/v1/api/tim-kiem";

    // Hand-written sample in the shape of the provider's list response (no real titles).
    private static final String LIST_PAGE_ONE_OF_THREE = """
            {"status": true, "msg": "done", "data": {
              "items": [
                {"name": "Movie One", "slug": "movie-one", "type": "single", "poster_url": "upload/one.jpg",
                 "year": 2024,
                 "category": [{"name": "Action", "slug": "action"}, {"name": "Drama", "slug": "drama"}]},
                {"name": "Cartoon Two", "slug": "cartoon-two", "type": "hoathinh",
                 "poster_url": "https://other.example/two.jpg", "year": 2023, "category": []}
              ],
              "params": {"pagination": {"totalItems": 41, "totalItemsPerPage": 20, "currentPage": 1, "totalPages": 3}},
              "APP_DOMAIN_CDN_IMAGE": "https://img.example"}}
            """;

    private FakeProviderServer server;
    private KKPhimProvider provider;

    @BeforeEach
    void startFakeProvider() {
        server = new FakeProviderServer();
        provider = new KKPhimProvider(server.baseUrl());
    }

    @AfterEach
    void stopFakeProvider() {
        server.close();
    }

    @Test
    void browseWithoutFiltersReadsTheNewestListAndMapsEachMovie() {
        server.respond(LIST_PATH, LIST_PAGE_ONE_OF_THREE);

        MoviePage page = provider.browse(new BrowseQuery(null, null, null, null, Paging.of(0, 20)));

        assertThat(page.items()).containsExactly(
                new MovieSummary("kkphim", "movie-one", "Movie One", MovieType.MOVIE,
                        "https://img.example/upload/one.jpg", 2024, List.of("Action", "Drama")),
                new MovieSummary("kkphim", "cartoon-two", "Cartoon Two", MovieType.SERIES,
                        "https://other.example/two.jpg", 2023, List.of()));
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void pageIsSentAsTheProviderPageWithTheSizeAsLimit() {
        server.respond(LIST_PATH, LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, null, null, null, Paging.of(2, 10)));

        assertThat(lastQuery()).containsEntry("page", "3").containsEntry("limit", "10");
    }

    @Test
    void typeSelectsTheListForThatType() {
        server.respond("/v1/api/danh-sach/phim-le", LIST_PAGE_ONE_OF_THREE);
        server.respond("/v1/api/danh-sach/phim-bo", LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, MovieType.MOVIE, null, null, Paging.of(0, 20)));
        provider.browse(new BrowseQuery(null, MovieType.SERIES, null, null, Paging.of(0, 20)));

        assertThat(server.requests()).extracting(URI::getPath)
                .containsExactly("/v1/api/danh-sach/phim-le", "/v1/api/danh-sach/phim-bo");
    }

    @Test
    void genreAndCountryAreSentAsProviderFilters() {
        server.respond(LIST_PATH, LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, null, "action", "korea", Paging.of(0, 20)));

        assertThat(lastQuery()).containsEntry("category", "action").containsEntry("country", "korea");
    }

    // The provider's search cannot filter by type, so the type is not applied when searching.
    @Test
    void aSearchUsesTheSearchEndpointWithTheKeyword() {
        server.respond(SEARCH_PATH, LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery("love story", MovieType.MOVIE, "action", null, Paging.of(0, 20)));

        assertThat(server.requests()).extracting(URI::getPath).containsExactly(SEARCH_PATH);
        assertThat(lastQuery()).containsEntry("keyword", "love story").containsEntry("category", "action");
    }

    @Test
    void aMovieWithMissingOptionalFieldsIsStillMapped() {
        server.respond(LIST_PATH, """
                {"status": true, "data": {
                  "items": [{"name": "Bare", "slug": "bare"}],
                  "params": {"pagination": {"currentPage": 1, "totalPages": 1}}}}
                """);

        MoviePage page = provider.browse(new BrowseQuery(null, null, null, null, Paging.of(0, 20)));

        assertThat(page.items()).containsExactly(
                new MovieSummary("kkphim", "bare", "Bare", MovieType.SERIES, null, null, List.of()));
    }

    private Map<String, String> lastQuery() {
        URI uri = server.requests().get(server.requests().size() - 1);
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0],
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }

}
