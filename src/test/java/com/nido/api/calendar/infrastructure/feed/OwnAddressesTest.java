package com.nido.api.calendar.infrastructure.feed;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OwnAddressesTest {

    @Test
    void theHostsOfTheDatabaseAndRedisAreReadOffTheirAddresses() {
        assertThat(OwnAddresses.hostsOf("jdbc:postgresql://postgres:5432/nido")).containsExactly("postgres");
        assertThat(OwnAddresses.hostsOf("jdbc:postgresql://db1:5432,db2/nido?ssl=true")).containsExactly("db1", "db2");
        assertThat(OwnAddresses.hostsOf("jdbc:postgresql://[::1]:5432/nido")).containsExactly("::1");
        assertThat(OwnAddresses.hostsOf("redis://:s3cr@t@redis:6379")).containsExactly("redis");
        assertThat(OwnAddresses.hostsOf("rediss://default:secret@cache.example:6380/0")).containsExactly("cache.example");
        assertThat(OwnAddresses.hostsOf(null)).isEmpty();
        assertThat(OwnAddresses.hostsOf("not a url")).isEmpty();
    }

    @Test
    void anAddressOfThisMachinesOwnInterfacesOrOfAServiceIsNidosOwn() throws UnknownHostException {
        OwnAddresses own = new OwnAddresses(List.of("localhost", "no-such-service.invalid"));

        assertThat(own.test(InetAddress.getByName("127.0.0.1"))).isTrue();
        assertThat(own.test(InetAddress.getByName("192.0.2.1"))).as("a documentation address nothing here holds").isFalse();
    }

    @Test
    void aServiceIsKnownByWhatItsNameGives() throws UnknownHostException {
        // A database given by an address no interface of this machine holds.
        InetAddress database = InetAddress.getByName("192.0.2.33");

        assertThat(new OwnAddresses(List.of()).test(database)).isFalse();
        assertThat(new OwnAddresses(List.of("192.0.2.33")).test(database)).isTrue();
    }
}
