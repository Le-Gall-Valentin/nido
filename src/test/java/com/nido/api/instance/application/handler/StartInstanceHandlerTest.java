package com.nido.api.instance.application.handler;

import com.nido.api.instance.domain.model.EnvironmentSeed;
import com.nido.api.instance.domain.model.InitialAdmin;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.KeyFilePort;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartInstanceHandlerTest {

    private final InstanceFakes.MemoryState state = new InstanceFakes.MemoryState();
    private final InstanceFakes.MemoryStore store = new InstanceFakes.MemoryStore();
    private final InstanceFakes.FakeMail mail = new InstanceFakes.FakeMail();
    private final InstanceFakes.MemoryCodes codes = new InstanceFakes.MemoryCodes();
    private final InstanceFakes.FakeAdmin admins = new InstanceFakes.FakeAdmin();
    private final Map<SettingKey, String> environment = new HashMap<>();
    private final KeyFilePort keyFile = new KeyFilePort() {
        @Override public Optional<String> read() { return Optional.empty(); }
        @Override public String create() { return "k"; }
        @Override public String location() { return "/data/secrets/encryption-key"; }
    };
    private final StartInstanceHandler handler = new StartInstanceHandler(() -> environment, store, mail, state, admins,
        codes, keyFile, Clock.systemUTC());

    private static final EnvironmentSeed NO_SEED = new EnvironmentSeed(null, null, null);

    @Test
    void an_installation_waiting_for_its_setup_gets_a_code() {
        assertThat(handler.start(NO_SEED)).isPresent();
        assertThat(codes.current()).isPresent();
        assertThat(admins.created).isEmpty();
    }

    @Test
    void an_installation_already_set_up_gets_none_and_ignores_the_seed() {
        state.setupCompleted = true;

        assertThat(handler.start(new EnvironmentSeed("admin", "admin@example.fr", "changeme1"))).isEmpty();
        assertThat(codes.current()).isEmpty();
        assertThat(admins.created).isEmpty();
    }

    @Test
    void the_three_seed_variables_set_the_installation_up_without_a_screen() {
        assertThat(handler.start(new EnvironmentSeed("admin", "admin@example.fr", "changeme1"))).isEmpty();

        assertThat(state.setupCompleted).isTrue();
        assertThat(admins.created).containsExactly(new InitialAdmin("admin", "admin@example.fr", "changeme1", null));
    }

    @Test
    void part_of_a_seed_stops_the_start() {
        assertThatThrownBy(() -> handler.start(new EnvironmentSeed("admin", null, null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NIDO_SEED_USERNAME, NIDO_SEED_EMAIL and NIDO_SEED_PASSWORD go together");
    }

    @Test
    void a_seed_password_shorter_than_eight_characters_stops_the_start() {
        assertThatThrownBy(() -> handler.start(new EnvironmentSeed("admin", "admin@example.fr", "short")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NIDO_SEED_PASSWORD");
    }

    @Test
    void a_wrong_value_in_the_environment_stops_the_start_and_names_the_variable() {
        environment.put(SettingKey.MAIL_PORT, "abc");

        assertThatThrownBy(() -> handler.start(NO_SEED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NIDO_SMTP_PORT: not_a_number");
    }

    @Test
    void mail_from_the_environment_without_a_public_address_stops_the_start() {
        environment.put(SettingKey.MAIL_HOST, "smtp.example.com");
        environment.put(SettingKey.MAIL_FROM, "nido@example.com");

        assertThatThrownBy(() -> handler.start(NO_SEED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NIDO_APP_URL: public_url_required_by_mail");
    }
}
