package com.nido.api.calendar.infrastructure.feed;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class AddressPolicyTest {

    /** A nido with no address of its own a test could reach. */
    static final Predicate<InetAddress> NOTHING_OF_NIDO = address -> false;

    private final AddressPolicy strict = AddressPolicy.standard(() -> false, NOTHING_OF_NIDO);
    private final AddressPolicy localNetwork = AddressPolicy.standard(() -> true, NOTHING_OF_NIDO);

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "127.8.9.10", "::1", "0.0.0.0", "::", "169.254.169.254", "fe80::1",
        "224.0.0.1", "ff02::1", "255.255.255.255", "240.0.0.1", "::ffff:127.0.0.1", "64:ff9b::7f00:1", "::7f00:1", "::ffff:169.254.169.254",
        "fd00:ec2::254", "100.100.100.200", "2002:c000:204::1", "2001:0:4136:e378:8000:63bf:3fff:fdd2", "64:ff9b:1::1",
        "::ffff:0:102:304"})
    void neverAnAddressOfTheMachineOrItsLink(String address) throws UnknownHostException {
        assertThat(strict.allows(InetAddress.getByName(address))).isFalse();
        assertThat(localNetwork.allows(InetAddress.getByName(address))).as("even with the local network open").isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.1.2.3", "172.16.0.1", "172.31.255.255", "192.168.1.10", "100.64.0.1", "fc00::1",
        "fd12:3456::1", "::ffff:192.168.1.10", "64:ff9b::a01:203", "192.0.0.8", "198.18.0.1", "198.19.255.255"})
    void theLocalNetworkOnlyWhenTheAdministratorOpensIt(String address) throws UnknownHostException {
        assertThat(strict.allows(InetAddress.getByName(address))).isFalse();
        assertThat(localNetwork.allows(InetAddress.getByName(address))).isTrue();
    }

    @org.junit.jupiter.api.Test
    void aSettingThatCannotBeReadIsASettingThatIsOff() throws UnknownHostException {
        AddressPolicy unreadable = AddressPolicy.standard(() -> {
            throw new IllegalStateException("settings unavailable");
        }, NOTHING_OF_NIDO);
        assertThat(unreadable.allows(InetAddress.getByName("10.1.2.3"))).isFalse();
        assertThat(unreadable.allows(InetAddress.getByName("8.8.8.8"))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "172.32.0.1", "100.128.0.1", "2001:4860:4860::8888", "142.250.75.238"})
    void anAddressOnTheInternet(String address) throws UnknownHostException {
        assertThat(strict.allows(InetAddress.getByName(address))).isTrue();
    }

    @org.junit.jupiter.api.Test
    void nidoItselfStaysClosedWithTheLocalNetworkOpen() throws UnknownHostException {
        // The database in the Docker network, and a public address of the machine's own interface.
        InetAddress database = InetAddress.getByName("172.31.250.3");
        InetAddress ownPublic = InetAddress.getByName("203.0.113.7");
        AddressPolicy open = AddressPolicy.standard(() -> true, address -> address.equals(database) || address.equals(ownPublic));

        assertThat(open.allows(database)).isFalse();
        assertThat(open.allows(ownPublic)).isFalse();
        assertThat(open.allows(InetAddress.getByName("::ffff:172.31.250.3"))).as("carried in IPv6").isFalse();
        assertThat(open.allows(InetAddress.getByName("172.31.250.4"))).as("a neighbour on the local network").isTrue();
        assertThat(open.allows(InetAddress.getByName("192.168.1.20"))).as("a Nextcloud at home").isTrue();
    }
}
