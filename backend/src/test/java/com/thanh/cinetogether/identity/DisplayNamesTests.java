package com.thanh.cinetogether.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// Rules: docs/design/database.md §3.1.
class DisplayNamesTests {

    private static final UUID USER_ID = UUID.fromString("fed78413-4614-4767-aeda-e57947638141");

    @Test
    void metadataNameIsUsedTrimmed() {
        assertThat(DisplayNames.initialDisplayName(USER_ID, "  Minh Nguyen  ", "minh@example.com"))
                .isEqualTo("Minh Nguyen");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void blankMetadataNameFallsBackToEmailLocalPart(String metadataName) {
        assertThat(DisplayNames.initialDisplayName(USER_ID, metadataName, "minh.nguyen@example.com"))
                .isEqualTo("minh.nguyen");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-at-sign", "@example.com", "   @example.com"})
    void unusableEmailFallsBackToViewerPlusFirstSixCharactersOfUserId(String email) {
        assertThat(DisplayNames.initialDisplayName(USER_ID, null, email)).isEqualTo("Viewer-fed784");
    }

    @Test
    void nameLongerThanEightyCharactersIsTruncated() {
        String name = "a".repeat(100);

        assertThat(DisplayNames.initialDisplayName(USER_ID, name, null)).isEqualTo("a".repeat(80));
    }

    @Test
    void longEmailLocalPartIsTruncatedToo() {
        String email = "b".repeat(100) + "@example.com";

        assertThat(DisplayNames.initialDisplayName(USER_ID, null, email)).isEqualTo("b".repeat(80));
    }

    // An emoji is two UTF-16 units; the column limit counts characters.
    @Test
    void truncationCountsWholeCharactersNotUtf16Units() {
        String name = "😀".repeat(100);

        String result = DisplayNames.initialDisplayName(USER_ID, name, null);

        assertThat(result.codePointCount(0, result.length())).isEqualTo(80);
        assertThat(result).isEqualTo("😀".repeat(80));
    }

}
