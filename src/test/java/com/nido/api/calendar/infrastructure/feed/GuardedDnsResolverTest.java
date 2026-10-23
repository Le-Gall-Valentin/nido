package com.nido.api.calendar.infrastructure.feed;

import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuardedDnsResolverTest {

    private static final Duration CONNECT = Duration.ofSeconds(5);
    private static final Duration TOTAL = Duration.ofSeconds(15);

    private final DnsResolver names = mock(DnsResolver.class);
    private final GuardedDnsResolver resolver = new GuardedDnsResolver(names, AddressPolicy.standard(() -> false, AddressPolicyTest.NOTHING_OF_NIDO), CONNECT, TOTAL);

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

    @Test
    void aNameGivingOneRefusedAddressAmongOthersGivesNone() throws Exception {
        when(names.resolve("rebind.example")).thenReturn(addresses("8.8.8.8", "10.0.0.5", "1.1.1.1"));

        assertThatThrownBy(() -> resolver.resolve("rebind.example", 443))
            .isInstanceOf(GuardedDnsResolver.BlockedAddressException.class);
    }

    @Test
    void theFamiliesAlternateSoThatTheCutNeverDropsTheOnlyOneThatAnswers() throws Exception {
        when(names.resolve("dual.example")).thenReturn(addresses("2001:4860:4860::8888", "2001:4860:4860::8844", "8.8.8.8",
            "8.8.4.4"));
        resolver.deadline(null);

        assertThat(resolver.resolve("dual.example", 443)).extracting(address -> address.getAddress().getHostAddress())
            .containsExactly("2001:4860:4860:0:0:0:0:8888", "8.8.8.8", "2001:4860:4860:0:0:0:0:8844");
    }

    @Test
    void anAddressWrittenOutIsJudgedAsItIsWithoutAskingTheNameService() throws Exception {
        assertThatThrownBy(() -> resolver.resolve("[::1]")).isInstanceOf(GuardedDnsResolver.BlockedAddressException.class);
        assertThatThrownBy(() -> resolver.resolve("[::7f00:1]")).isInstanceOf(GuardedDnsResolver.BlockedAddressException.class);
        assertThat(resolver.resolve("[2001:4860:4860::8888]")).hasSize(1);
        assertThat(resolver.resolve("8.8.8.8")).hasSize(1);
        verifyNoInteractions(names);
    }

    @Test
    void anIpv4WrittenInIpv6NotationIsRefusedWhoeverReadsIt() throws Exception {
        // Not a literal to httpcore: the name service is asked, and gives the loopback it names.
        when(names.resolve("::ffff:127.0.0.1")).thenReturn(addresses("::ffff:127.0.0.1"));

        assertThatThrownBy(() -> resolver.resolve("::ffff:127.0.0.1")).isInstanceOf(GuardedDnsResolver.BlockedAddressException.class);
    }

    @Test
    void aNameServiceThatDoesNotAnswerIsWaitedForNoLongerThanTheDownloadHasLeft() throws Exception {
        CountDownLatch never = new CountDownLatch(1);
        DnsResolver silent = mock(DnsResolver.class);
        when(silent.resolve("slow.example")).thenAnswer(call -> {
            never.await();
            return addresses("8.8.8.8");
        });
        GuardedDnsResolver bounded = new GuardedDnsResolver(silent, AddressPolicy.standard(() -> false,
            AddressPolicyTest.NOTHING_OF_NIDO), Duration.ofMillis(50), Duration.ofMillis(200));
        bounded.deadline(System.nanoTime() + Duration.ofMillis(100).toNanos());
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThatThrownBy(() -> bounded.resolve("slow.example")).isInstanceOf(UnknownHostException.class));
        } finally {
            never.countDown();
        }
    }

    private static InetAddress[] addresses(String... literals) throws Exception {
        InetAddress[] out = new InetAddress[literals.length];
        for (int i = 0; i < literals.length; i++) {
            out[i] = InetAddress.getByName(literals[i]);
        }
        return out;
    }
}
