package com.nido.api.calendar.infrastructure.feed;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * nido itself, as a followed calendar must never reach it: every address of its own network interfaces — the machine's,
 * or the container's —, and those of the database and the Redis it uses. Looked up when an address is judged: a
 * container started again on another address is still known. A service name that no longer resolves names nothing.
 */
final class OwnAddresses implements Predicate<InetAddress> {

    private final List<String> serviceHosts;

    OwnAddresses(List<String> serviceHosts) {
        this.serviceHosts = List.copyOf(serviceHosts);
    }

    /**
     * The hosts of a JDBC URL ({@code jdbc:postgresql://db1:5432,db2/nido}) or of a Redis one
     * ({@code redis://:secret@redis:6379}); none for a URL nido cannot read, or none given.
     */
    static List<String> hostsOf(String url) {
        if (url == null || url.isBlank()) {
            return List.of();
        }
        String bare = url.startsWith("jdbc:") ? url.substring("jdbc:".length()) : url;
        int start = bare.indexOf("//");
        if (start < 0) {
            return List.of();
        }
        String authority = bare.substring(start + 2).split("[/?]", 2)[0];
        authority = authority.substring(authority.lastIndexOf('@') + 1);
        List<String> hosts = new ArrayList<>();
        for (String hostAndPort : authority.split(",")) {
            String host = URI.create("scheme://" + hostAndPort).getHost();
            if (host != null && !host.isEmpty()) {
                hosts.add(host.startsWith("[") ? host.substring(1, host.length() - 1) : host);
            }
        }
        return hosts;
    }

    @Override
    public boolean test(InetAddress address) {
        return interfaces().contains(address) || services().contains(address);
    }

    private static Set<InetAddress> interfaces() {
        Set<InetAddress> own = new HashSet<>();
        try {
            for (NetworkInterface each : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                own.addAll(Collections.list(each.getInetAddresses()));
            }
        } catch (SocketException unreadable) {
            // The machine's interfaces cannot be listed: loopback and link stay closed by the policy itself.
        }
        return own;
    }

    private Set<InetAddress> services() {
        Set<InetAddress> own = new HashSet<>();
        for (String host : serviceHosts) {
            try {
                own.addAll(List.of(InetAddress.getAllByName(host)));
            } catch (UnknownHostException gone) {
                // A service name that does not resolve here is no address a download could reach by it.
            }
        }
        return own;
    }
}
