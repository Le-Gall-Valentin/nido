package com.nido.api.infrastructure.sealing;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvelopeTest {

    private static final String HERE = "tasks.title_encrypted:6f1d7c4e-0000-4000-8000-000000000001";
    private static final String THERE = "tasks.title_encrypted:6f1d7c4e-0000-4000-8000-000000000002";

    private static int bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    @Test
    void any_value_comes_back_exactly() {
        for (String value : new String[] {"", "Lait", "a|b|c", "ligne 1\nligne 2", "Crème fraîche 🥛", "🥚".repeat(1000)}) {
            assertThat(Envelope.unwrap(HERE, Envelope.wrap(HERE, value))).isEqualTo(value);
        }
    }

    @Test
    void a_value_that_fills_its_bucket_exactly_comes_back_without_padding() {
        for (String value : new String[] {"x".repeat(32), "x".repeat(64), "é".repeat(16), "🥚".repeat(8)}) {
            String envelope = Envelope.wrap(HERE, value);

            assertThat(envelope).doesNotEndWith("#");
            assertThat(Envelope.unwrap(HERE, envelope)).isEqualTo(value);
        }
    }

    @Test
    void a_value_ending_like_the_padding_keeps_its_own_end() {
        for (String value : new String[] {"#", "C#", "##", "Recette n°3 #" + "#".repeat(18), "#".repeat(32)}) {
            assertThat(Envelope.unwrap(HERE, Envelope.wrap(HERE, value))).isEqualTo(value);
        }
    }

    @Test
    void a_length_of_nine_characters_that_are_not_all_digits_is_refused_as_no_envelope() {
        for (String length : new String[] {"00000000x", "-00000005", "+00000005", "0000 0005"}) {
            String broken = HERE + "|" + length + "|Loyer" + "#".repeat(27);

            assertThatThrownBy(() -> Envelope.unwrap(HERE, broken)).isInstanceOf(EnvelopeRejected.class);
        }
    }

    @Test
    void every_amount_takes_the_same_room() {
        assertThat(bytes(Envelope.wrap(HERE, "3.50")))
            .isEqualTo(bytes(Envelope.wrap(HERE, "12500.00")))
            .isEqualTo(bytes(Envelope.wrap(HERE, "1500000.00")));
    }

    @Test
    void a_short_and_a_long_text_of_one_bucket_take_the_same_room() {
        // The length written in the envelope must not give away by its width what the padding hides.
        assertThat(bytes(Envelope.wrap(HERE, "Pain"))).isEqualTo(bytes(Envelope.wrap(HERE, "Courses du samedi")));
        assertThat(bytes(Envelope.wrap(HERE, "x".repeat(99)))).isEqualTo(bytes(Envelope.wrap(HERE, "x".repeat(100))));
    }

    @Test
    void a_value_past_32_bytes_moves_to_the_next_bucket() {
        int thirtyTwo = bytes(Envelope.wrap(HERE, "x".repeat(32)));
        int thirtyThree = bytes(Envelope.wrap(HERE, "x".repeat(33)));

        assertThat(thirtyThree - thirtyTwo).isEqualTo(32); // the bucket doubles: 32 bytes of value and padding become 64
    }

    @Test
    void an_envelope_of_another_place_is_refused_and_says_whose_it_is() {
        String envelope = Envelope.wrap(THERE, "Loyer");

        assertThatThrownBy(() -> Envelope.unwrap(HERE, envelope))
            .isInstanceOf(EnvelopeRejected.class)
            .satisfies(e -> assertThat(((EnvelopeRejected) e).claimedReference()).contains(THERE));
    }

    @Test
    void a_text_that_is_not_an_envelope_is_refused() {
        String envelope = Envelope.wrap(HERE, "Loyer");

        for (String broken : new String[] {"Loyer", HERE + "|5", HERE + "|x|Loyer", HERE + "|5|Loyer" + "#".repeat(27),
                envelope.substring(0, envelope.length() - 1), envelope.substring(0, envelope.length() - 1) + "*"}) {
            assertThatThrownBy(() -> Envelope.unwrap(HERE, broken))
                .isInstanceOf(EnvelopeRejected.class)
                .satisfies(e -> assertThat(((EnvelopeRejected) e).claimedReference()).isEmpty());
        }
    }

    @Test
    void a_reference_cannot_hold_the_separator() {
        assertThatThrownBy(() -> Envelope.wrap("a|b", "x")).isInstanceOf(IllegalArgumentException.class);
    }
}
