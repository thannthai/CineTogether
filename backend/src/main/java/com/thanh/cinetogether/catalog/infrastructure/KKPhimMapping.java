package com.thanh.cinetogether.catalog.infrastructure;

import java.net.URI;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.thanh.cinetogether.catalog.domain.movie.EpisodeProgress;
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

    // Only an http(s) link to a .m3u8 playlist is a usable HLS source; the link comes from a third party.
    static boolean isHlsUrl(String link) {
        if (link == null || link.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(link);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getPath() != null && uri.getPath().endsWith(".m3u8");
        } catch (IllegalArgumentException e) {
            return false;
        }
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


    private static final Pattern PROGRESS_FRACTION = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern EPISODE_NUMBER = Pattern.compile("(?i)tập\\s*(\\d+)");

    // The provider says "Tập 8", "Hoàn Tất (8/8)" or "Full" in one text, the planned total and the status apart.
    // A label that says nothing about episodes (a trailer, "Đang cập nhật") gives no progress.
    static EpisodeProgress progress(String label, String total, String status) {
        String text = label == null ? "" : label.strip();
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("full")) {
            return new EpisodeProgress(null, null, true);
        }

        Integer current = null;
        Integer planned = positiveNumber(total);
        Matcher fraction = PROGRESS_FRACTION.matcher(text);
        Matcher number = EPISODE_NUMBER.matcher(text);
        if (fraction.find()) {
            current = Integer.valueOf(fraction.group(1));
            planned = Integer.valueOf(fraction.group(2));
        } else if (number.find()) {
            current = Integer.valueOf(number.group(1));
        }
        if (current == null) {
            return EpisodeProgress.NONE;
        }
        boolean completed = lower.contains("hoàn tất") || "completed".equalsIgnoreCase(status);
        return new EpisodeProgress(current, planned, completed);
    }

    private static Integer positiveNumber(String text) {
        if (text == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(text.strip());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }


    // The provider sends empty or blank text for what it does not know.
    static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    // Names of people: blank entries and the provider's placeholder for "not known yet" are dropped.
    static java.util.List<String> names(java.util.List<String> names) {
        if (names == null) {
            return java.util.List.of();
        }
        return names.stream()
                .map(KKPhimMapping::blankToNull)
                .filter(name -> name != null && !name.equalsIgnoreCase("Đang cập nhật"))
                .toList();
    }

    // The trailer comes from a third party and is shown as a link or an embed: only http(s) is accepted.
    static String trailerUrl(String link) {
        String clean = blankToNull(link);
        if (clean == null) {
            return null;
        }
        try {
            String scheme = URI.create(clean).getScheme();
            return "http".equals(scheme) || "https".equals(scheme) ? clean : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
