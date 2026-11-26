package com.thanh.cinetogether.catalog.infrastructure;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The parts of the KKPhim JSON responses that the adapter reads. Other fields are ignored. */
final class KKPhimResponses {

    private KKPhimResponses() {
    }

    record ListResponse(Data data) {
    }

    record Data(List<Item> items, Params params, @JsonProperty("APP_DOMAIN_CDN_IMAGE") String cdnImageDomain) {
    }

    record Params(Pagination pagination) {
    }

    record Pagination(int currentPage, int totalPages) {
    }

    record Item(String name, @JsonProperty("origin_name") String originName, String slug, String type,
            @JsonProperty("poster_url") String posterUrl, Integer year, String quality, String lang,
            @JsonProperty("episode_current") String episodeCurrent, List<Category> category) {
    }

    record Category(String name) {
    }

    record DetailResponse(Movie movie, List<Server> episodes) {
    }

    record Movie(String name, @JsonProperty("origin_name") String originName, String slug, String type, String content,
            @JsonProperty("poster_url") String posterUrl, @JsonProperty("thumb_url") String thumbUrl, Integer year,
            String quality, String lang, String time, @JsonProperty("episode_current") String episodeCurrent,
            @JsonProperty("episode_total") String episodeTotal, String status, Integer view, List<String> actor,
            List<String> director, @JsonProperty("trailer_url") String trailerUrl, List<Category> category) {
    }

    record Server(@JsonProperty("server_data") List<EpisodeItem> serverData) {
    }

    record EpisodeItem(String name, String slug, @JsonProperty("link_m3u8") String linkM3u8) {
    }

}
