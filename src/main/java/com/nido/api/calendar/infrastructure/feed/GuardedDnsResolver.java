package com.nido.api.calendar.infrastructure.feed;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.core5.net.InetAddressUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The only way the download client learns an address: every address a name gives must be allowed, or none is used.
 * The client connects to what this returned — a name cannot answer "public" here and "127.0.0.1" at the connection.
 */
final class GuardedDnsResolver implements DnsResolver {

    /** Thrown for a name giving an address the policy refuses. An UnknownHostException, so the client stops right there. */
    static final class BlockedAddressException extends UnknownHostException {
        BlockedAddressException(String host) {
            super(host + " gives an address nido does not download from");
        }
    }

    private final DnsResolver delegate;
    private final AddressPolicy policy;
    private final long connectNanos;
    private final long totalNanos;
    /** When the download in progress on this thread must be over (System.nanoTime), or null outside a download. */
    private final ThreadLocal<Long> deadline = new ThreadLocal<>();

    GuardedDnsResolver(DnsResolver delegate, AddressPolicy policy, Duration connect, Duration total) {
        this.delegate = delegate;
        this.policy = policy;
        this.connectNanos = Math.max(1, connect.toNanos());
        this.totalNanos = total.toNanos();
    }

    void deadline(Long nanoTime) {
        if (nanoTime == null) {
            deadline.remove();
        } else {
            deadline.set(nanoTime);
        }
    }

    /**
     * Connecting cannot be cancelled mid-attempt: only as many addresses are handed over as the time left in this download
     * holds connect timeouts — and never more than the whole download would. Alternating the families, so that the cut
     * cannot drop the only family that answers.
     */
    private int attempts() {
        int most = (int) Math.max(1, totalNanos / connectNanos);
        Long end = deadline.get();
        return end == null ? most : (int) Math.min(most, Math.max(1, (end - System.nanoTime()) / connectNanos));
    }

    private static List<InetAddress> interleaved(InetAddress[] addresses) {
        List<InetAddress> v6 = Arrays.stream(addresses).filter(a -> a instanceof Inet6Address).toList();
        List<InetAddress> v4 = Arrays.stream(addresses).filter(a -> !(a instanceof Inet6Address)).toList();
        List<InetAddress> mixed = new ArrayList<>();
        for (int i = 0; i < Math.max(v6.size(), v4.size()); i++) {
            if (i < v6.size()) {
                mixed.add(v6.get(i));
            }
            if (i < v4.size()) {
                mixed.add(v4.get(i));
            }
        }
        return mixed;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        InetAddress[] addresses = literal(host).orElse(null);
        if (addresses == null) {
            addresses = delegate.resolve(host);
        }
        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException(host);
        }
        if (!Arrays.stream(addresses).allMatch(policy::allows)) {
            throw new BlockedAddressException(host);
        }
        return addresses;
    }

    /** An address written out needs no name service: it is judged as it is, brackets of an IPv6 one removed. */
    private static Optional<InetAddress[]> literal(String host) {
        String bare = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        if (!InetAddressUtils.isIPv4Address(bare) && !InetAddressUtils.isIPv6Address(bare)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new InetAddress[] {InetAddress.getByName(bare)});
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<InetSocketAddress> resolve(String host, int port) throws UnknownHostException {
        return interleaved(resolve(host)).stream().limit(attempts()).map(address -> new InetSocketAddress(address, port)).toList();
    }

    @Override
    public String resolveCanonicalHostname(String host) throws UnknownHostException {
        return delegate.resolveCanonicalHostname(host);
    }
}
