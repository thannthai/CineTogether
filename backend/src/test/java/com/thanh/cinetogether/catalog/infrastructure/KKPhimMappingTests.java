package com.thanh.cinetogether.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.thanh.cinetogether.catalog.domain.movie.EpisodeProgress;
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


    // KKPhim writes the progress as text: "Tập 8", "Hoàn Tất (8/8)" or "Full"; the total and the status come apart.
    @Test
    void aCompletedSeriesCarriesTheNumbersInTheLabel() {
        assertThat(KKPhimMapping.progress("Hoàn Tất (8/8)", null, null)).isEqualTo(new EpisodeProgress(8, 8, true));
        assertThat(KKPhimMapping.progress("HOÀN TẤT (3/3)", "3", "completed"))
                .isEqualTo(new EpisodeProgress(3, 3, true));
    }

    @Test
    void anOngoingSeriesHasTheLatestEpisodeAndTheTotalWhenKnown() {
        assertThat(KKPhimMapping.progress("Tập 8", "24", "ongoing")).isEqualTo(new EpisodeProgress(8, 24, false));
        assertThat(KKPhimMapping.progress("Tập 8", null, null)).isEqualTo(new EpisodeProgress(8, null, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "?", "", "abc", "-3"})
    void aTotalThatIsNotAPositiveNumberIsUnknown(String total) {
        assertThat(KKPhimMapping.progress("Tập 8", total, "ongoing").total()).isNull();
    }

    @Test
    void theStatusCanMarkASeriesCompletedWithoutTheWordInTheLabel() {
        assertThat(KKPhimMapping.progress("Tập 36", "36", "completed")).isEqualTo(new EpisodeProgress(36, 36, true));
    }

    @Test
    void aSingleMovieIsFullWithoutNumbers() {
        assertThat(KKPhimMapping.progress("Full", null, null)).isEqualTo(new EpisodeProgress(null, null, true));
        assertThat(KKPhimMapping.progress("FULL", "1", "completed")).isEqualTo(new EpisodeProgress(null, null, true));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Trailer", "Đang cập nhật", "Sắp chiếu"})
    void aLabelWithoutProgressGivesNothing(String label) {
        assertThat(KKPhimMapping.progress(label, "0", "trailer")).isEqualTo(EpisodeProgress.NONE);
    }

}
