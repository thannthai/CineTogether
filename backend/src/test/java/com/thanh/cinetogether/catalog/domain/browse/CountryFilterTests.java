package com.thanh.cinetogether.catalog.domain.browse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class CountryFilterTests {

    @Test
    void missingOrEmptyValuesMeanNoFilter() {
        assertThat(CountryFilter.of(null).slugs()).isEmpty();
        assertThat(CountryFilter.of(List.of()).slugs()).isEmpty();
    }

    @Test
    void blankAndRepeatedValuesAreDroppedAndTheOrderIsKept() {
        CountryFilter filter = CountryFilter.of(List.of("han-quoc", " ", "trung-quoc", "han-quoc"));

        assertThat(filter.slugs()).containsExactly("han-quoc", "trung-quoc");
    }

    @Test
    void fiveCountriesAreAcceptedAndSixAreRejected() {
        assertThat(CountryFilter.of(List.of("a", "b", "c", "d", "e")).slugs()).hasSize(5);

        assertThatThrownBy(() -> CountryFilter.of(List.of("a", "b", "c", "d", "e", "f")))
                .isInstanceOf(InvalidFilterException.class);
    }

    @Test
    void onlyLowerCaseSlugsAreAccepted() {
        assertThat(CountryFilter.of(List.of("han-quoc", "au-my", "uae")).slugs()).hasSize(3);

        for (String bad : List.of("Han Quoc", "HAN-QUOC", "han_quoc", "a,b", "-a", "a-", "a--b", "../x")) {
            assertThatThrownBy(() -> CountryFilter.of(List.of(bad)))
                    .as(bad)
                    .isInstanceOf(InvalidFilterException.class);
        }
    }

    @Test
    void repeatedValuesDoNotCountTowardsTheLimit() {
        assertThat(CountryFilter.of(List.of("a", "a", "a", "a", "a", "a", "b")).slugs()).containsExactly("a", "b");
    }

}
