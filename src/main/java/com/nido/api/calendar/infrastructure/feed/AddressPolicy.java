package com.nido.api.calendar.infrastructure.feed;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Which addresses a followed calendar may be downloaded from. Never this machine, its link (169.254.169.254 is a cloud's
 * metadata), a multicast group or a reserved block; the local network only when the administrator opens it. An IPv6
 * address carrying an IPv4 one — mapped, compatible, NAT64 — is judged on the IPv4 it carries.
 */
@FunctionalInterface
public interface AddressPolicy {

    boolean allows(InetAddress address);

    static AddressPolicy standard(BooleanSupplier allowPrivateNetwork) {
        return address -> {
            InetAddress target = embeddedIpv4(address).orElse(address);
            if (alwaysBlocked(target)) {
                return false;
            }
            return !privateNetwork(target) || allowPrivateNetwork.getAsBoolean();
        };
    }

    private static boolean alwaysBlocked(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }
        if (address instanceof Inet4Address) {
            int first = address.getAddress()[0] & 0xff;
            return first == 0 || first >= 240; // "this network", and the reserved block up to the broadcast address
        }
        return false;
    }

    private static boolean privateNetwork(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int a = bytes[0] & 0xff;
            int b = bytes[1] & 0xff;
            return a == 10 || a == 172 && b >= 16 && b <= 31 || a == 192 && b == 168 || a == 100 && b >= 64 && b <= 127;
        }
        return (bytes[0] & 0xfe) == 0xfc || address.isSiteLocalAddress(); // fc00::/7, and the old fec0::/10
    }

    private static Optional<InetAddress> embeddedIpv4(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return Optional.empty();
        }
        byte[] b = address.getAddress();
        boolean firstTenZero = Arrays.equals(Arrays.copyOfRange(b, 0, 10), new byte[10]);
        boolean mapped = firstTenZero && b[10] == (byte) 0xff && b[11] == (byte) 0xff;
        boolean compatible = firstTenZero && b[10] == 0 && b[11] == 0;
        boolean nat64 = b[0] == 0x00 && b[1] == 0x64 && b[2] == (byte) 0xff && b[3] == (byte) 0x9b
            && Arrays.equals(Arrays.copyOfRange(b, 4, 12), new byte[8]);
        if (!mapped && !compatible && !nat64) {
            return Optional.empty();
        }
        try {
            return Optional.of(InetAddress.getByAddress(Arrays.copyOfRange(b, 12, 16)));
        } catch (UnknownHostException impossible) {
            return Optional.empty();
        }
    }
}
