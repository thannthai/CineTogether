package com.thanh.cinetogether.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.browse.BrowseQuery;
import com.thanh.cinetogether.catalog.domain.browse.CountryFilter;
import com.thanh.cinetogether.catalog.domain.browse.MovieList;
import com.thanh.cinetogether.catalog.domain.movie.Episode;
import com.thanh.cinetogether.catalog.domain.movie.EpisodeProgress;
import com.thanh.cinetogether.catalog.domain.movie.MovieDetail;
import com.thanh.cinetogether.catalog.domain.movie.MoviePage;
import com.thanh.cinetogether.catalog.domain.movie.PlaybackSource;
import com.thanh.cinetogether.catalog.domain.movie.StreamFormat;
import com.thanh.cinetogether.catalog.domain.movie.MovieSummary;
import com.thanh.cinetogether.catalog.domain.movie.MovieType;
import com.thanh.cinetogether.catalog.domain.browse.Paging;

class KKPhimProviderTests {

    private static final String LIST_PATH = "/v1/api/danh-sach/phim-moi-cap-nhat";
    private static final String SEARCH_PATH = "/v1/api/tim-kiem";

    // Hand-written sample in the shape of the provider's list response (no real titles).
    private static final String LIST_PAGE_ONE_OF_THREE = """
            {"status": true, "msg": "done", "data": {
              "items": [
                {"name": "Movie One", "slug": "movie-one", "type": "single", "poster_url": "upload/one.jpg",
                 "year": 2024, "imdb": {"id": "tt1", "vote_average": 7.1, "vote_count": 1200},
                 "tmdb": {"id": "1", "vote_average": 7, "vote_count": 5},
                 "category": [{"name": "Action", "slug": "action"}, {"name": "Drama", "slug": "drama"}]},
                {"name": "Cartoon Two", "slug": "cartoon-two", "type": "hoathinh",
                 "poster_url": "https://other.example/two.jpg", "year": 2023, "category": [],
                 "imdb": {"id": null, "vote_average": 0, "vote_count": 0},
                 "tmdb": {"id": "2", "vote_average": 8, "vote_count": 40}}
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
                new MovieSummary("kkphim", "movie-one", "Movie One", null, MovieType.MOVIE,
                        "https://img.example/upload/one.jpg", 2024, List.of("Action", "Drama"), null, null,
                        EpisodeProgress.NONE, 1200),
                new MovieSummary("kkphim", "cartoon-two", "Cartoon Two", null, MovieType.SERIES,
                        "https://other.example/two.jpg", 2023, List.of(), null, null, EpisodeProgress.NONE, 40));
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
    void theCinemaListReadsTheProvidersCinemaList() {
        server.respond("/v1/api/danh-sach/phim-chieu-rap", LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, null, null, null, MovieList.CINEMA, Paging.of(0, 20)));

        assertThat(server.requests()).extracting(URI::getPath).containsExactly("/v1/api/danh-sach/phim-chieu-rap");
    }

    @Test
    void theAnimeListReadsTheProvidersAnimationList() {
        server.respond("/v1/api/danh-sach/hoat-hinh", LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, null, null, null, MovieList.ANIME, Paging.of(0, 20)));

        assertThat(server.requests()).extracting(URI::getPath).containsExactly("/v1/api/danh-sach/hoat-hinh");
    }

    @Test
    void theRatingCountComesFromTheListWithoutAnyDetailCall() {
        server.respond(LIST_PATH, LIST_PAGE_ONE_OF_THREE);

        List<MovieSummary> movies = provider.browse(FIRST_PAGE).items();

        // IMDb when it has votes, else TMDb.
        assertThat(movies).extracting(MovieSummary::ratingCount).containsExactly(1200, 40);
        assertThat(server.requests()).hasSize(1);
    }

    @Test
    void theYearIsSentAsAProviderFilterOnlyWhenGiven() {
        server.respond(LIST_PATH, LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, null, "family", null, null, 2026, Paging.of(0, 20)));
        assertThat(lastQuery()).containsEntry("year", "2026").containsEntry("category", "family");

        provider.browse(FIRST_PAGE);
        assertThat(lastQuery()).doesNotContainKey("year");
    }

    @Test
    void theCinemaListWinsOverTheType() {
        server.respond("/v1/api/danh-sach/phim-chieu-rap", LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, MovieType.SERIES, null, null, MovieList.CINEMA, Paging.of(0, 20)));

        assertThat(server.requests()).extracting(URI::getPath).containsExactly("/v1/api/danh-sach/phim-chieu-rap");
    }

    @Test
    void aSearchIgnoresTheList() {
        server.respond(SEARCH_PATH, LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery("love", null, null, null, MovieList.CINEMA, Paging.of(0, 20)));

        assertThat(server.requests()).extracting(URI::getPath).containsExactly(SEARCH_PATH);
    }

    @Test
    void genreAndCountryAreSentAsProviderFilters() {
        server.respond(LIST_PATH, LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, null, "action", CountryFilter.of(List.of("korea")),
                Paging.of(0, 20)));

        assertThat(lastQuery()).containsEntry("category", "action").containsEntry("country", "korea");
    }

    @Test
    void severalCountriesAreSentAsOneCommaSeparatedFilter() {
        server.respond(LIST_PATH, LIST_PAGE_ONE_OF_THREE);

        provider.browse(new BrowseQuery(null, null, null, CountryFilter.of(List.of("korea", "japan")),
                Paging.of(0, 20)));

        assertThat(lastQuery()).containsEntry("country", "korea,japan");
    }

    @Test
    void noCountryMeansNoCountryParameter() {
        server.respond(LIST_PATH, LIST_PAGE_ONE_OF_THREE);

        provider.browse(FIRST_PAGE);

        assertThat(lastQuery()).doesNotContainKey("country");
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

    @Test
    void detailMapsTheMovieAndItsEpisodes() {
        server.respond("/phim/movie-one", """
                {"status": true, "msg": "done",
                 "movie": {"name": "Movie One", "slug": "movie-one", "type": "series",
                           "content": "<p>First <b>line</b></p><p>Second &amp; last</p>",
                           "poster_url": "https://img.example/one.jpg",
                           "thumb_url": "https://img.example/one-thumb.jpg", "category": [{"name": "Drama"}]},
                 "episodes": [{"server_name": "A", "server_data": [
                   {"name": "Tập 1", "slug": "tap-1"}, {"name": "Tập 2", "slug": "tap-2"}]}]}
                """);

        Optional<MovieDetail> detail = provider.detail("movie-one");

        assertThat(detail).contains(new MovieDetail("kkphim", "movie-one", "Movie One", MovieType.SERIES,
                "First line\nSecond & last", "https://img.example/one.jpg", "https://img.example/one-thumb.jpg",
                List.of("Drama"),
                List.of(new Episode("tap-1", "Tập 1"), new Episode("tap-2", "Tập 2"))));
    }

    @Test
    void browseMapsTheNamesTagsAndProgressOfEachMovie() {
        server.respond(LIST_PATH, """
                {"data": {"items": [
                  {"name": "Thứ Nữ", "origin_name": "Noble Daughter", "slug": "thu-nu", "type": "series",
                   "poster_url": "https://img.example/thu-nu.jpg", "year": 2026, "quality": "FHD", "lang": "Vietsub",
                   "episode_current": "Tập 8", "category": []},
                  {"name": "Under Siege", "slug": "under-siege", "type": "series", "year": 2026,
                   "episode_current": "Hoàn Tất (8/8)", "quality": " ", "lang": "", "category": []},
                  {"name": "Dune", "origin_name": "Dune: Part Two", "slug": "dune", "type": "single",
                   "episode_current": "Full", "category": []}],
                 "params": {"pagination": {"currentPage": 1, "totalPages": 1}}}}
                """);

        List<MovieSummary> movies = provider.browse(new BrowseQuery(null, null, null, null, Paging.of(null, null)))
                .items();

        assertThat(movies.get(0).originalTitle()).isEqualTo("Noble Daughter");
        assertThat(movies.get(0).quality()).isEqualTo("FHD");
        assertThat(movies.get(0).language()).isEqualTo("Vietsub");
        assertThat(movies.get(0).progress()).isEqualTo(new EpisodeProgress(8, null, false));
        assertThat(movies.get(1).originalTitle()).isNull();
        assertThat(movies.get(1).quality()).as("a blank value is unknown").isNull();
        assertThat(movies.get(1).language()).isNull();
        assertThat(movies.get(1).progress()).isEqualTo(new EpisodeProgress(8, 8, true));
        assertThat(movies.get(2).progress()).isEqualTo(new EpisodeProgress(null, null, true));
    }

    @Test
    void detailMapsTheExtraInformation() {
        server.respond("/phim/thu-nu", """
                {"movie": {"name": "Thứ Nữ", "origin_name": "Noble Daughter", "slug": "thu-nu", "type": "series",
                           "year": 2026, "quality": "FHD", "lang": "Vietsub", "time": "19 phút/tập",
                           "episode_current": "Tập 8", "episode_total": "24", "status": "ongoing", "view": 77,
                           "actor": ["Richard Li", "  ", "Wang Junbi"], "director": ["Đang cập nhật"],
                           "trailer_url": "https://www.youtube.com/watch?v=abc123", "category": []},
                 "episodes": []}
                """);

        MovieDetail detail = provider.detail("thu-nu").orElseThrow();

        assertThat(detail.originalTitle()).isEqualTo("Noble Daughter");
        assertThat(detail.releaseYear()).isEqualTo(2026);
        assertThat(detail.quality()).isEqualTo("FHD");
        assertThat(detail.language()).isEqualTo("Vietsub");
        assertThat(detail.duration()).isEqualTo("19 phút/tập");
        assertThat(detail.progress()).isEqualTo(new EpisodeProgress(8, 24, false));
        assertThat(detail.views()).isEqualTo(77);
        assertThat(detail.actors()).as("blank names are dropped").containsExactly("Richard Li", "Wang Junbi");
        assertThat(detail.directors()).as("a placeholder is not a name").isEmpty();
        assertThat(detail.trailerUrl()).isEqualTo("https://www.youtube.com/watch?v=abc123");
    }

    @Test
    void aTrailerThatIsNotAnHttpLinkIsDropped() {
        server.respond("/phim/a", "{\"movie\": {\"name\": \"A\", \"slug\": \"a\", \"trailer_url\": \"javascript:alert(1)\"}}");
        server.respond("/phim/b", "{\"movie\": {\"name\": \"B\", \"slug\": \"b\", \"trailer_url\": \"\"}}");

        assertThat(provider.detail("a").orElseThrow().trailerUrl()).isNull();
        assertThat(provider.detail("b").orElseThrow().trailerUrl()).isNull();
    }

    @Test
    void episodesOfSeveralServersAreMergedOncePerSlugInOrder() {
        server.respond("/phim/movie-one", """
                {"movie": {"name": "Movie One", "slug": "movie-one", "type": "series"},
                 "episodes": [
                   {"server_name": "A", "server_data": [
                     {"name": "Tập 1", "slug": "tap-1"}, {"name": "Tập 2", "slug": "tap-2"}]},
                   {"server_name": "B", "server_data": [
                     {"name": "Tập 2 (B)", "slug": "tap-2"}, {"name": "Tập 3", "slug": "tap-3"}]}]}
                """);

        Optional<MovieDetail> detail = provider.detail("movie-one");

        assertThat(detail.orElseThrow().episodes()).containsExactly(
                new Episode("tap-1", "Tập 1"), new Episode("tap-2", "Tập 2"), new Episode("tap-3", "Tập 3"));
    }

    @Test
    void aDetailWithMissingOptionalFieldsIsStillMapped() {
        server.respond("/phim/bare", "{\"movie\": {\"name\": \"Bare\", \"slug\": \"bare\"}}");

        Optional<MovieDetail> detail = provider.detail("bare");

        assertThat(detail).contains(new MovieDetail("kkphim", "bare", "Bare", MovieType.SERIES, null, null,
                null, List.of(), List.of()));
    }

    @Test
    void aMovieTheProviderDoesNotKnowIsEmpty() {
        assertThat(provider.detail("missing")).isEmpty();
    }

    private static final String TWO_SERVERS = """
            {"movie": {"name": "Movie One", "slug": "movie-one"},
             "episodes": [
               {"server_name": "A", "server_data": [
                 {"name": "Tập 1", "slug": "tap-1", "link_embed": "https://player.example/a1"},
                 {"name": "Tập 2", "slug": "tap-2", "link_m3u8": "https://a.example/2.m3u8"}]},
               {"server_name": "B", "server_data": [
                 {"name": "Tập 1", "slug": "tap-1", "link_m3u8": "https://b.example/1.m3u8"},
                 {"name": "Tập 2", "slug": "tap-2", "link_m3u8": "https://b.example/2.m3u8"}]}]}
            """;

    @Test
    void playbackSourceIsTheHlsLinkOfTheFirstServerThatHasOne() {
        server.respond("/phim/movie-one", TWO_SERVERS);

        assertThat(provider.playbackSource("movie-one", "tap-2")).contains(new PlaybackSource("kkphim", "movie-one",
                "tap-2", "https://a.example/2.m3u8", StreamFormat.HLS));
    }

    @Test
    void aServerWithoutAnHlsLinkForTheEpisodeIsSkipped() {
        server.respond("/phim/movie-one", TWO_SERVERS);

        assertThat(provider.playbackSource("movie-one", "tap-1").orElseThrow().streamUrl())
                .isEqualTo("https://b.example/1.m3u8");
    }

    @Test
    void anUnknownEpisodeOrMovieIsEmpty() {
        server.respond("/phim/movie-one", TWO_SERVERS);

        assertThat(provider.playbackSource("movie-one", "tap-9")).isEmpty();
        assertThat(provider.playbackSource("missing", "tap-1")).isEmpty();
    }

    // Only an http(s) link to a .m3u8 playlist is a usable HLS source; anything else is not trusted.
    @ParameterizedTest
    @ValueSource(strings = {"https://player.example/embed", "javascript:alert(1)", "https://x.example/video.mp4",
            "ftp://x.example/a.m3u8", ""})
    void aLinkThatIsNotAnHlsPlaylistGivesNoSource(String link) {
        server.respond("/phim/movie-one", "{\"movie\": {\"name\": \"M\", \"slug\": \"movie-one\"}, \"episodes\": "
                + "[{\"server_data\": [{\"name\": \"Tập 1\", \"slug\": \"tap-1\", \"link_m3u8\": \"" + link + "\"}]}]}");

        assertThat(provider.playbackSource("movie-one", "tap-1")).isEmpty();
    }

    private static final BrowseQuery FIRST_PAGE = new BrowseQuery(null, null, null, null, Paging.of(0, 20));

    static Stream<Arguments> everyOperation() {
        return Stream.of(
                Arguments.of(Named.<Consumer<KKPhimProvider>>of("browse", p -> p.browse(FIRST_PAGE))),
                Arguments.of(Named.<Consumer<KKPhimProvider>>of("detail", p -> p.detail("movie-one"))),
                Arguments.of(Named.<Consumer<KKPhimProvider>>of("playbackSource",
                        p -> p.playbackSource("movie-one", "tap-1"))));
    }

    @ParameterizedTest
    @MethodSource("everyOperation")
    void aServerErrorMeansTheProviderIsUnavailable(Consumer<KKPhimProvider> operation) {
        server.respond(LIST_PATH, 500, "oops");
        server.respond("/phim/movie-one", 500, "oops");

        assertThatThrownBy(() -> operation.accept(provider)).isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void aResponseThatIsNotJsonMeansTheProviderIsUnavailable() {
        server.respond(LIST_PATH, "<html>oops</html>");

        assertThatThrownBy(() -> provider.browse(FIRST_PAGE)).isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void aListResponseWithoutDataMeansTheProviderIsUnavailable() {
        server.respond(LIST_PATH, "{\"status\": false, \"msg\": \"nope\"}");

        assertThatThrownBy(() -> provider.browse(FIRST_PAGE)).isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void aProviderSlowerThanTheReadTimeoutMeansItIsUnavailable() {
        server.respondSlowly(LIST_PATH, LIST_PAGE_ONE_OF_THREE, 1000);
        KKPhimProvider impatient = new KKPhimProvider(server.baseUrl(), Duration.ofSeconds(1), Duration.ofMillis(200));

        assertThatThrownBy(() -> impatient.browse(FIRST_PAGE)).isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void aProviderThatRefusesTheConnectionMeansItIsUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        KKPhimProvider unreachable = new KKPhimProvider("http://localhost:" + closedPort);

        assertThatThrownBy(() -> unreachable.browse(FIRST_PAGE)).isInstanceOf(ProviderUnavailableException.class);
    }

    private Map<String, String> lastQuery() {
        URI uri = server.requests().get(server.requests().size() - 1);
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0],
                        pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }

}
