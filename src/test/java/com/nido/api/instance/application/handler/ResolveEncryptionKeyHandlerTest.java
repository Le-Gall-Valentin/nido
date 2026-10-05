package com.nido.api.instance.application.handler;

import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.KeyFingerprint;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import com.nido.api.instance.domain.port.out.KeyFilePort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResolveEncryptionKeyHandlerTest {

    private static final String KEY = "the-key-of-this-installation-32chars";

    static final class FakeState implements InstanceStatePort {
        KeyFingerprint fingerprint;
        boolean generated;
        boolean setupCompleted;

        @Override public InstanceState load() { return new InstanceState(Optional.ofNullable(fingerprint), generated, setupCompleted); }
        @Override public void recordFingerprint(KeyFingerprint f, boolean g) { fingerprint = f; generated = g; }
        @Override public boolean markSetupCompleted(Instant at) { boolean was = setupCompleted; setupCompleted = true; return !was; }
    }

    static final class FakeFile implements KeyFilePort {
        String content;
        @Override public Optional<String> read() { return Optional.ofNullable(content); }
        @Override public String create() { content = "generated-key-of-forty-four-characters-xxxx="; return content; }
        @Override public String location() { return "/data/secrets/encryption-key"; }
    }

    private final FakeState state = new FakeState();
    private final FakeFile file = new FakeFile();
    private final ResolveEncryptionKeyHandler handler = new ResolveEncryptionKeyHandler(state, file);

    @Test
    void a_fresh_installation_without_key_generates_one_and_remembers_it_was_generated() {
        String key = handler.resolve(Optional.empty());

        assertThat(key).isEqualTo(file.content);
        assertThat(state.generated).isTrue();
        assertThat(state.fingerprint.matches(key)).isTrue();
    }

    @Test
    void the_configured_key_wins_over_the_file() {
        file.content = "a-different-key-of-at-least-32-characters";

        assertThat(handler.resolve(Optional.of(KEY))).isEqualTo(KEY);
        assertThat(state.fingerprint.matches(KEY)).isTrue();
        assertThat(state.generated).isFalse();
    }

    @Test
    void a_blank_configured_key_counts_as_none() {
        file.content = KEY;

        assertThat(handler.resolve(Optional.of("  "))).isEqualTo(KEY);
    }

    @Test
    void the_file_is_used_on_the_next_start() {
        String first = handler.resolve(Optional.empty());

        assertThat(handler.resolve(Optional.empty())).isEqualTo(first);
    }

    @Test
    void a_generated_key_lost_before_the_setup_is_done_is_replaced_by_a_new_one() {
        handler.resolve(Optional.empty());
        file.content = null;

        String again = handler.resolve(Optional.empty());

        assertThat(again).isEqualTo(file.content);
        assertThat(state.generated).isTrue();
        assertThat(state.fingerprint.matches(again)).isTrue();
    }

    @Test
    void a_key_found_in_the_data_directory_without_fingerprint_is_shown_as_generated() {
        // The start that wrote it stopped before recording its fingerprint: the setup must still show it.
        file.content = "generated-key-of-forty-four-characters-xxxx=";

        handler.resolve(Optional.empty());

        assertThat(state.generated).isTrue();
        assertThat(state.fingerprint.matches(file.content)).isTrue();
    }

    @Test
    void a_refusal_stops_the_start() {
        state.setupCompleted = true;

        assertThatThrownBy(() -> handler.resolve(Optional.empty()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("/data/secrets/encryption-key");
    }
}
