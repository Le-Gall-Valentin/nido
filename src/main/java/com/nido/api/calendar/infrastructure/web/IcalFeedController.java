package com.nido.api.calendar.infrastructure.web;

import com.nido.api.calendar.application.port.in.ReadSubscriptionFeedUseCase;
import com.nido.api.infrastructure.ratelimit.RateLimitMode;
import com.nido.api.infrastructure.ratelimit.RateLimiting;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one route a calendar application reads, with no account: the token in the path is the whole credential. Every
 * way of not reading — unknown, revoked, owner gone — answers the same 404, and nothing tells a search engine to keep it.
 */
@RestController
@Hidden
public class IcalFeedController {

    private final ReadSubscriptionFeedUseCase readUseCase;

    public IcalFeedController(ReadSubscriptionFeedUseCase readUseCase) {
        this.readUseCase = readUseCase;
    }

    // Counted per IP address, across every link: a calendar application signs in as nobody. Google and Apple poll far below this.
    @GetMapping("/ical/{token}.ics")
    @RateLimiting(mode = RateLimitMode.IP, max = 60)
    public ResponseEntity<String> read(@PathVariable String token) {
        return readUseCase.read(token)
            .map(content -> ResponseEntity.ok()
                .contentType(IcalFiles.TEXT_CALENDAR)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ZERO).cachePrivate())
                .header("X-Robots-Tag", "noindex")
                .header("Referrer-Policy", "no-referrer")
                .body(content))
            .orElseGet(() -> ResponseEntity.notFound().header("X-Robots-Tag", "noindex").build());
    }
}
