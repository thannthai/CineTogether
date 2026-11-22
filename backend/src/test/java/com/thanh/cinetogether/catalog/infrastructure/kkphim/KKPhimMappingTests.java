package com.thanh.cinetogether.catalog.infrastructure.kkphim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.thanh.cinetogether.catalog.domain.movie.MovieType;
import com.thanh.cinetogether.catalog.domain.browse.Paging;

class KKPhimMappingTests {

    @Test
    void singleIsAMovie() {
        assertThat(KKPhimMapping.movieType("single")).isEqualTo(MovieType.MOVIE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"series", "hoathinh", "tvshows", "something-new"})
    void everyOtherTypeIsASeries(String providerType) {
        assertThat(KKPhimMapping.movieType(providerType)).isEqualTo(MovieType.SERIES);
    }

    // Our pages start at 0, the provider's at 1.
    @Test
    void providerPageIsOurPagePlusOne() {
        assertThat(KKPhimMapping.providerPage(new Paging(0, 20))).isEqualTo(1);
        assertThat(KKPhimMapping.providerPage(new Paging(4, 20))).isEqualTo(5);
    }

    @Test
    void hasNextIsTrueOnlyBeforeTheLastPage() {
        assertThat(KKPhimMapping.hasNext(1, 5)).isTrue();
        assertThat(KKPhimMapping.hasNext(5, 5)).isFalse();
        assertThat(KKPhimMapping.hasNext(1, 0)).isFalse();
    }

    @Test
    void relativeImagePathIsJoinedToTheCdnBaseWithOneSlash() {
        assertThat(KKPhimMapping.imageUrl("https://cdn.example", "uploads/a.jpg"))
                .isEqualTo("https://cdn.example/uploads/a.jpg");
        assertThat(KKPhimMapping.imageUrl("https://cdn.example/", "/uploads/a.jpg"))
                .isEqualTo("https://cdn.example/uploads/a.jpg");
    }

    @Test
    void absoluteImageUrlIsKept() {
        assertThat(KKPhimMapping.imageUrl("https://cdn.example", "https://other.example/a.jpg"))
                .isEqualTo("https://other.example/a.jpg");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void missingImagePathGivesNoImage(String path) {
        assertThat(KKPhimMapping.imageUrl("https://cdn.example", path)).isNull();
    }

    @Test
    void relativePathWithoutACdnBaseGivesNoImage() {
        assertThat(KKPhimMapping.imageUrl(null, "uploads/a.jpg")).isNull();
        assertThat(KKPhimMapping.imageUrl("  ", "uploads/a.jpg")).isNull();
    }

    @Test
    void inlineTagsAreRemovedWithoutAddingSpaces() {
        assertThat(KKPhimMapping.plainText("Hel<b>lo</b> <i>world</i>")).isEqualTo("Hello world");
    }

    @Test
    void paragraphsAndLineBreaksBecomeNewlines() {
        assertThat(KKPhimMapping.plainText("<p>First</p><p>Second</p>")).isEqualTo("First\nSecond");
        assertThat(KKPhimMapping.plainText("One<br/>Two<br>Three")).isEqualTo("One\nTwo\nThree");
    }

    @Test
    void scriptAndStyleElementsAreRemovedWithTheirContent() {
        assertThat(KKPhimMapping.plainText("<script>alert(1)</script>Hi<style>p{}</style>")).isEqualTo("Hi");
    }

    @Test
    void commonEntitiesAreDecoded() {
        assertThat(KKPhimMapping.plainText("Tom &amp; Jerry &quot;x&quot; &lt;3 &nbsp;ok"))
                .isEqualTo("Tom & Jerry \"x\" <3 ok");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "<p> </p>"})
    void textWithNothingVisibleGivesNull(String html) {
        assertThat(KKPhimMapping.plainText(html)).isNull();
    }

}
