package com.nido.api.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * The address a rate-limit bucket is charged to.
 *
 * <p>This used to parse {@code X-Forwarded-For} itself, trusting it only when the connecting peer
 * appeared in a {@code nido.rate-limit.trusted-proxies} list. The guard was sound and the list was
 * empty — it appeared in no configuration file and had no default — so the header was never read
 * and every request behind a reverse proxy was charged to the proxy. Measured on a live deployment:
 * two clients on different continents, one bucket, {@code 172.18.0.1}. Every public endpoint's
 * limit was therefore a limit on the whole application at once, and five requests a minute could
 * keep everyone from logging in.
 *
 * <p>So the parsing is gone and {@code server.forward-headers-strategy=NATIVE} does it instead.
 * Tomcat's {@code RemoteIpValve} rewrites {@code getRemoteAddr()} before anything here runs, taking
 * the rightmost address the header offers that is not itself a known proxy — which is correct for a
 * chain of them, where taking the last entry was only ever correct for exactly one. It trusts the
 * header only from a peer inside the private ranges, so a client reaching the application directly
 * cannot forge one, and everything else that asks for the caller's address — logs included — gets
 * the right answer too rather than only this class.
 *
 * <p>Kept as a seam rather than inlined: the interceptor should not have to know where the address
 * comes from, and a test can still substitute one.
 */
@Component
public class ClientIpResolver {

    public String resolve(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
