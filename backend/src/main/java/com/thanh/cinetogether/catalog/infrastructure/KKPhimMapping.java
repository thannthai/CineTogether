package com.thanh.cinetogether.catalog.infrastructure;

import com.thanh.cinetogether.catalog.domain.MovieType;
import com.thanh.cinetogether.catalog.domain.Paging;

final class KKPhimMapping {

    private KKPhimMapping() {
    }

    // The provider also has hoathinh (animation) and tvshows; the API only knows MOVIE and SERIES.
    static MovieType movieType(String providerType) {
        return "single".equals(providerType) ? MovieType.MOVIE : MovieType.SERIES;
    }

    // Our pages start at 0, the provider's at 1.
    static int providerPage(Paging paging) {
        return paging.page() + 1;
    }

    static boolean hasNext(int currentPage, int totalPages) {
        return currentPage < totalPages;
    }

    // The provider sends image paths relative to a CDN base in some responses and full URLs in others.
    static String imageUrl(String cdnBase, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith("http")) {
            return path;
        }
        if (cdnBase == null || cdnBase.isBlank()) {
            return null;
        }
        return cdnBase.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

}
