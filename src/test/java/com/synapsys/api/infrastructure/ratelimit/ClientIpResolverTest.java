package com.synapsys.api.infrastructure.ratelimit;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private ClientIpResolver resolver(String... trustedProxies) {
        SynapsysProperties.RateLimitProperties rateLimit =
            new SynapsysProperties.RateLimitProperties(List.of(trustedProxies));
        SynapsysProperties props = new SynapsysProperties(
            null, null, null, null, null, rateLimit, null, null
        );
        return new ClientIpResolver(props);
    }

    @Test
    void returnsRemoteAddrWhenNoTrustedProxy() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("1.2.3.4");
        req.addHeader("X-Forwarded-For", "9.9.9.9, 1.2.3.4");

        assertThat(resolver().resolve(req)).isEqualTo("1.2.3.4");
    }

    @Test
    void returnsLastXffElementWhenRemoteAddrIsTrusted() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1"); // trusted proxy
        req.addHeader("X-Forwarded-For", "10.0.0.1, real-client");

        assertThat(resolver("10.0.0.1").resolve(req)).isEqualTo("real-client");
    }

    @Test
    void attackerCannotForgeIpWhenProxyAppends() {
        // Attacker sends X-Forwarded-For: forged-ip
        // Proxy appends real client IP: "forged-ip, real-client"
        // Last element = real-client (added by trusted proxy)
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "forged-ip, real-client");

        assertThat(resolver("10.0.0.1").resolve(req)).isEqualTo("real-client");
    }

    @Test
    void returnsRemoteAddrWhenXffAbsent() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");

        assertThat(resolver("10.0.0.1").resolve(req)).isEqualTo("10.0.0.1");
    }

    @Test
    void trimsWhitespaceFromXffElement() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "10.0.0.1 , client-ip");

        assertThat(resolver("10.0.0.1").resolve(req)).isEqualTo("client-ip");
    }

    @Test
    void fallsBackToRemoteAddrWhenXffContainsOnlyCommas() {
        // Malformed XFF like " , " passes the isBlank() check but yields an empty extracted IP
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", " , ");

        assertThat(resolver("10.0.0.1").resolve(req)).isEqualTo("10.0.0.1");
    }
}