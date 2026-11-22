package com.thanh.cinetogether.catalog.infrastructure.kkphim;

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

    record Item(String name, String slug, String type, @JsonProperty("poster_url") String posterUrl, Integer year,
            List<Category> category) {
    }

    record Category(String name) {
    }

    record DetailResponse(Movie movie, List<Server> episodes) {
    }

    record Movie(String name, String slug, String type, String content, @JsonProperty("poster_url") String posterUrl,
            List<Category> category) {
    }

    record Server(@JsonProperty("server_data") List<EpisodeItem> serverData) {
    }

    record EpisodeItem(String name, String slug) {
    }

}
