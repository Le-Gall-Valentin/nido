package com.nido.api.calendar.infrastructure.feed;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.core5.net.InetAddressUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
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
    /** Connecting cannot be cancelled mid-attempt: as many addresses are tried as the total time holds connect timeouts. */
    private final int maxAttempts;

    GuardedDnsResolver(DnsResolver delegate, AddressPolicy policy, int maxAttempts) {
        this.maxAttempts = maxAttempts;
        this.delegate = delegate;
        this.policy = policy;
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
        return Arrays.stream(resolve(host)).limit(maxAttempts).map(address -> new InetSocketAddress(address, port)).toList();
    }

    @Override
    public String resolveCanonicalHostname(String host) throws UnknownHostException {
        return delegate.resolveCanonicalHostname(host);
    }
}
