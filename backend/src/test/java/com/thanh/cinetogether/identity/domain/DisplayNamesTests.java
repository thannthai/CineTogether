package com.thanh.cinetogether.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    // An emoji is two UTF-16 units; the column limit counts characters.
    @Test
    void truncationCountsWholeCharactersNotUtf16Units() {
        String name = "😀".repeat(100);

        String result = DisplayNames.initialDisplayName(USER_ID, name, null);

        assertThat(result.codePointCount(0, result.length())).isEqualTo(80);
        assertThat(result).isEqualTo("😀".repeat(80));
    }

    @Test
    void validatedNameIsReturnedTrimmed() {
        assertThat(DisplayNames.validated("  Minh Nguyen  ")).isEqualTo("Minh Nguyen");
    }

    @Test
    void nameOfExactlyEightyCharactersIsValid() {
        assertThat(DisplayNames.validated("a".repeat(80))).isEqualTo("a".repeat(80));
    }

    @Test
    void lengthIsMeasuredAfterTrimming() {
        String name = "a".repeat(80);

        assertThat(DisplayNames.validated("   " + name + "   ")).isEqualTo(name);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void missingOrBlankNameIsRejected(String name) {
        assertThatThrownBy(() -> DisplayNames.validated(name)).isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void nameLongerThanEightyCharactersIsRejected() {
        assertThatThrownBy(() -> DisplayNames.validated("a".repeat(81)))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    // The limit counts characters, not UTF-16 units: an emoji is one character.
    @Test
    void eightyEmojiAreValid() {
        assertThat(DisplayNames.validated("😀".repeat(80))).isEqualTo("😀".repeat(80));
    }

    @Test
    void eightyOneEmojiAreRejected() {
        assertThatThrownBy(() -> DisplayNames.validated("😀".repeat(81)))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    // Zero-width space, non-breaking spaces, and a lone zero-width joiner all render as nothing.
    @ParameterizedTest
    @ValueSource(strings = {"\u200B", "\u00A0\u00A0", "\u200D"})
    void nameWithoutVisibleCharactersIsRejected(String name) {
        assertThatThrownBy(() -> DisplayNames.validated(name)).isInstanceOf(InvalidDisplayNameException.class);
    }

    // Zero-width space, right-to-left override, newline, NUL.
    @ParameterizedTest
    @ValueSource(strings = {"Minh\u200BNguyen", "\u202Eabc", "Minh\nNguyen", "Minh\0"})
    void nameWithControlOrInvisibleFormatCharactersIsRejected(String name) {
        assertThatThrownBy(() -> DisplayNames.validated(name)).isInstanceOf(InvalidDisplayNameException.class);
    }

    // The zero-width joiner is what glues emoji into one symbol, so it must stay allowed.
    @Test
    void emojiJoinedWithZeroWidthJoinerIsValid() {
        String family = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67";

        assertThat(DisplayNames.validated(family)).isEqualTo(family);
    }

    @Test
    void vietnameseNameWithCombiningMarksIsValid() {
        String name = "Nguye\u0302\u0303n Tha\u0301i";

        assertThat(DisplayNames.validated(name)).isEqualTo(name);
    }

    @Test
    void metadataNameWithoutVisibleCharactersFallsBackToEmailLocalPart() {
        assertThat(DisplayNames.initialDisplayName(USER_ID, "\u200B", "minh@example.com")).isEqualTo("minh");
    }

    @Test
    void metadataNameWithInvisibleFormatCharacterFallsBackToEmailLocalPart() {
        assertThat(DisplayNames.initialDisplayName(USER_ID, "Minh\u202E", "minh@example.com")).isEqualTo("minh");
    }

}
