package com.nido.api.instance.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PublicUrlTest {

    @ParameterizedTest
    @CsvSource({
        "https://nido.example.com,         https://nido.example.com",
        "https://Nido.Example.com/,        https://nido.example.com",
        "HTTPS://nido.example.com,         https://nido.example.com",
        "http://192.168.1.10:8080,         http://192.168.1.10:8080",
        "http://localhost:5173/,           http://localhost:5173",
        "'  https://nido.example.com  ',   https://nido.example.com",
    })
    void an_address_is_kept_in_one_canonical_form(String typed, String kept) {
        assertThat(PublicUrl.parse(typed)).map(PublicUrl::value).contains(kept);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "nido.example.com", "ftp://nido.example.com", "https://", "https://jane:pw@nido.example.com",
        "https://nido.example.com/?a=b", "https://nido.example.com/#top", "mailto:jane@example.com", "https://exa mple.com",
        // Nido answers at the root of its host only: its pages, /api and the cookies' paths all start at /.
        "https://example.com/nido", "https://example.com/nido/"})
    void anything_else_is_not_an_address(String typed) {
        assertThat(PublicUrl.parse(typed)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"https://nido.example.com, true", "http://192.168.1.10:8080, false"})
    void it_knows_whether_it_is_secure(String url, boolean secure) {
        assertThat(PublicUrl.parse(url).orElseThrow().secure()).isEqualTo(secure);
    }
}
