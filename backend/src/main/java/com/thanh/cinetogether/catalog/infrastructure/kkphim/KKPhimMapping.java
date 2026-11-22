package com.thanh.cinetogether.catalog.infrastructure.kkphim;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.thanh.cinetogether.catalog.domain.movie.MovieType;
import com.thanh.cinetogether.catalog.domain.browse.Paging;

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

    // The description is third-party HTML. Return plain text so no client can be tricked into running markup.
    static String plainText(String html) {
        if (html == null) {
            return null;
        }
        String text = html
                .replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>", "")
                .replaceAll("(?i)<br\\s*/?>|</(p|div|li)\\s*>", "\n")
                .replaceAll("<[^>]*>", "");
        // Entities are decoded after the tags are removed, and &amp; last, so decoding cannot create markup.
        text = text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&amp;", "&");
        String result = Arrays.stream(text.replaceAll("[ \\t\\u00A0]+", " ").split("\n"))
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n"));
        return result.isEmpty() ? null : result;
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
