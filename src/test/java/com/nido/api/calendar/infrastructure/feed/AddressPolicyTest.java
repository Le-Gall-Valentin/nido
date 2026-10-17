package com.nido.api.calendar.infrastructure.feed;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class AddressPolicyTest {

    private final AddressPolicy strict = AddressPolicy.standard(() -> false);
    private final AddressPolicy localNetwork = AddressPolicy.standard(() -> true);

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "127.8.9.10", "::1", "0.0.0.0", "::", "169.254.169.254", "fe80::1",
        "224.0.0.1", "ff02::1", "255.255.255.255", "240.0.0.1", "::ffff:127.0.0.1", "64:ff9b::7f00:1", "::ffff:169.254.169.254"})
    void neverAnAddressOfTheMachineOrItsLink(String address) throws UnknownHostException {
        assertThat(strict.allows(InetAddress.getByName(address))).isFalse();
        assertThat(localNetwork.allows(InetAddress.getByName(address))).as("even with the local network open").isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.1.2.3", "172.16.0.1", "172.31.255.255", "192.168.1.10", "100.64.0.1", "fc00::1",
        "fd12:3456::1", "::ffff:192.168.1.10", "64:ff9b::a01:203"})
    void theLocalNetworkOnlyWhenTheAdministratorOpensIt(String address) throws UnknownHostException {
        assertThat(strict.allows(InetAddress.getByName(address))).isFalse();
        assertThat(localNetwork.allows(InetAddress.getByName(address))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "172.32.0.1", "100.128.0.1", "2001:4860:4860::8888", "142.250.75.238"})
    void anAddressOnTheInternet(String address) throws UnknownHostException {
        assertThat(strict.allows(InetAddress.getByName(address))).isTrue();
    }
}
