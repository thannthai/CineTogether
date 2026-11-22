package com.thanh.cinetogether.catalog.domain.movie;

import java.util.List;

public record MoviePage(List<MovieSummary> items, int page, int size, boolean hasNext) {
}
