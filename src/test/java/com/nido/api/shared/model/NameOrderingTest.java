package com.nido.api.shared.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NameOrderingTest {

    @Test
    void names_sort_as_a_french_reader_expects_case_and_accents_only_breaking_ties() {
        assertThat(List.of("Zeste", "éclair", "abricot", "Banane").stream().sorted(NameOrdering.comparator()).toList())
            .containsExactly("abricot", "Banane", "éclair", "Zeste");
    }
}
