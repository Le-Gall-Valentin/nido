package com.nido.api.calendar.infrastructure.feed;

import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingsResolution;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.net.InetAddress;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalFeedHttpConfigTest {

    /** The database and Redis on addresses no interface here holds, and that nothing else would refuse. */
    private final MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.datasource.url", "jdbc:postgresql://192.0.2.44:5432/nido")
        .withProperty("spring.data.redis.url", "redis://:secret@192.0.2.45:6379");

    private static GetEffectiveSettingsQuery localNetwork(String allowed) {
        EffectiveSettings settings = SettingsResolution.resolve(Map.of(),
            Map.of(SettingKey.CALENDAR_ALLOW_PRIVATE_NETWORK, allowed)).settings();
        return () -> settings;
    }

    @Test
    void theLocalNetworkSettingOpensTheLocalNetworkOfTheDownloads() throws Exception {
        InetAddress nextcloud = InetAddress.getByName("192.168.1.20");

        assertThat(ExternalFeedHttpConfig.policy(localNetwork("false"), environment).allows(nextcloud)).isFalse();
        assertThat(ExternalFeedHttpConfig.policy(localNetwork("true"), environment).allows(nextcloud)).isTrue();
    }

    @Test
    void theDatabaseAndRedisNidoIsConfiguredWithStayClosedWhateverTheSetting() throws Exception {
        AddressPolicy open = ExternalFeedHttpConfig.policy(localNetwork("true"), environment);

        assertThat(open.allows(InetAddress.getByName("192.0.2.44"))).isFalse();
        assertThat(open.allows(InetAddress.getByName("192.0.2.45"))).isFalse();
        assertThat(open.allows(InetAddress.getByName("192.0.2.46"))).isTrue();
    }

    @Test
    void aDatabaseAddressGivenAnotherWayNamesNoServiceAndStopsNothing() throws Exception {
        // The tests' containers give the connection without the variable the address is read from.
        MockEnvironment unresolved = new MockEnvironment().withProperty("spring.datasource.url", "${NIDO_DB_URL_MISSING}");

        AddressPolicy open = ExternalFeedHttpConfig.policy(localNetwork("true"), unresolved);

        assertThat(open.allows(InetAddress.getByName("192.168.1.20"))).isTrue();
    }
}
