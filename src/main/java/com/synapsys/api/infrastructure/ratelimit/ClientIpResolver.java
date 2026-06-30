package com.synapsys.api.infrastructure.ratelimit;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientIpResolver {

    private final List<String> trustedProxies;

    public ClientIpResolver(SynapsysProperties properties) {
        var rateLimit = properties.rateLimit();
        this.trustedProxies = rateLimit != null
            ? rateLimit.trustedProxies().stream().filter(s -> !s.isBlank()).toList()
            : List.of();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxies.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String[] parts = xff.split(",");
                // Last entry = IP appended by the trusted proxy. Assumes a single trusted
                // reverse proxy that appends the real client IP at the end of the XFF header.
                // (RFC 7239 leftmost-is-client convention is not used here.)
                String extracted = parts[parts.length - 1].trim();
                if (!extracted.isBlank()) return extracted;
            }
        }
        return remoteAddr;
    }
}
