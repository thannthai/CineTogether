package com.thanh.cinetogether.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PagingTests {

    @Test
    void missingValuesUseTheDefaults() {
        Paging paging = Paging.of(null, null);

        assertThat(paging.page()).isZero();
        assertThat(paging.size()).isEqualTo(20);
    }

    @Test
    void valuesAtTheLimitsAreAccepted() {
        assertThat(Paging.of(0, 1)).isEqualTo(new Paging(0, 1));
        assertThat(Paging.of(7, 50)).isEqualTo(new Paging(7, 50));
    }

    @Test
    void negativePageIsRejected() {
        assertThatThrownBy(() -> Paging.of(-1, 20)).isInstanceOf(InvalidPagingException.class);
    }

    @Test
    void sizeBelowOneOrAboveFiftyIsRejected() {
        assertThatThrownBy(() -> Paging.of(0, 0)).isInstanceOf(InvalidPagingException.class);
        assertThatThrownBy(() -> Paging.of(0, 51)).isInstanceOf(InvalidPagingException.class);
    }

}
