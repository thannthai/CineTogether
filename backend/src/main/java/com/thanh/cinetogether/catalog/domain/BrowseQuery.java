package com.thanh.cinetogether.catalog.domain;

/** Every filter is optional (null means "not filtered"). */
public record BrowseQuery(String q, MovieType type, String genre, String country, Paging paging) {
}
