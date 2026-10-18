package com.nido.api.calendar.infrastructure.feed;

import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuardedDnsResolverTest {

    private static final Duration CONNECT = Duration.ofSeconds(5);
    private static final Duration TOTAL = Duration.ofSeconds(15);

    private final DnsResolver names = mock(DnsResolver.class);
    private final GuardedDnsResolver resolver = new GuardedDnsResolver(names, AddressPolicy.standard(() -> false), CONNECT, TOTAL);

    @Test
    void aDownloadJustBegunTriesAsManyAddressesAsTheWholeDownloadHolds() throws Exception {
        when(names.resolve("agenda.example")).thenReturn(addresses("1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4"));
        // A few milliseconds in: what is left still holds the third connect timeout, cut by the deadline if need be.
        resolver.deadline(System.nanoTime() + TOTAL.toNanos() - Duration.ofMillis(5).toNanos());

        assertThat(resolver.resolve("agenda.example", 443)).hasSize(3);
    }

    @Test
    void aDownloadAlmostOverStillTriesOneAddress() throws Exception {
        when(names.resolve("agenda.example")).thenReturn(addresses("1.1.1.1", "1.0.0.1"));
        resolver.deadline(System.nanoTime() - 1);

        assertThat(resolver.resolve("agenda.example", 443)).hasSize(1);
    }

    @Test
    void outsideADownloadTheWholeDownloadSetsTheCap() throws Exception {
        when(names.resolve("agenda.example")).thenReturn(addresses("1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4"));
        resolver.deadline(null);

        assertThat(resolver.resolve("agenda.example", 443)).hasSize(3);
    }

    private static InetAddress[] addresses(String... literals) throws Exception {
        InetAddress[] out = new InetAddress[literals.length];
        for (int i = 0; i < literals.length; i++) {
            out[i] = InetAddress.getByName(literals[i]);
        }
        return out;
    }
}
