package com.thanh.cinetogether.catalog.domain.browse;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

/** Countries to include (any of them). An empty list means "not filtered". */
public record CountryFilter(List<String> slugs) {

    public static final int MAX_COUNTRIES = 5;

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public CountryFilter {
        if (slugs.size() > MAX_COUNTRIES) {
            throw new InvalidFilterException("At most " + MAX_COUNTRIES + " countries can be selected.");
        }
        for (String slug : slugs) {
            if (!SLUG.matcher(slug).matches()) {
                throw new InvalidFilterException("Invalid country: " + slug);
            }
        }
    }

    public static CountryFilter of(List<String> values) {
        if (values == null) {
            return new CountryFilter(List.of());
        }
        // The set drops repeats first, so they do not count towards the limit.
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                unique.add(value.strip());
            }
        }
        return new CountryFilter(List.copyOf(unique));
    }

}
