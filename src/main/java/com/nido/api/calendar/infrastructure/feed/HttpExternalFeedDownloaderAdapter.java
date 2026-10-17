package com.nido.api.calendar.infrastructure.feed;

import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.infrastructure.NidoVersion;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.io.ModalCloseable;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads a followed calendar: http(s) only, every address checked through {@link GuardedDnsResolver} — redirects
 * followed by hand, each one checked again —, 5 s to connect and 15 s in all, 5 MB at most counted as it arrives and
 * once unpacked, no cookie, no credential. An error is a code, never a word of what the server answered.
 */
public final class HttpExternalFeedDownloaderAdapter implements ExternalFeedDownloader, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(HttpExternalFeedDownloaderAdapter.class);

    public record Limits(Duration connect, Duration total, int maxBytes, int maxRedirects) {
        public static Limits standard() {
            return new Limits(Duration.ofSeconds(5), Duration.ofSeconds(15), 5 * 1024 * 1024, 3);
        }
    }

    private static final Set<Integer> REDIRECTS = Set.of(301, 302, 303, 307, 308);

    private final GuardedDnsResolver resolver;
    private final Limits limits;
    private final CloseableHttpClient client;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "feed-download-deadline");
        thread.setDaemon(true);
        return thread;
    });

    public HttpExternalFeedDownloaderAdapter(AddressPolicy policy, DnsResolver dns, Limits limits) {
        this.resolver = new GuardedDnsResolver(dns, policy,
            (int) Math.max(1, limits.total().toMillis() / Math.max(1, limits.connect().toMillis())));
        this.limits = limits;
        this.client = HttpClients.custom()
            .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(resolver)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.of(limits.connect()))
                    .setSocketTimeout(Timeout.of(limits.total()))
                    .build())
                .build())
            .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(Timeout.of(limits.total())).build())
            .disableRedirectHandling()
            .disableAutomaticRetries()
            .setConnectionReuseStrategy((request, response, context) -> false) // every download is checked afresh
            .disableCookieManagement()
            .disableAuthCaching()
            .setUserAgent("nido/" + NidoVersion.current())
            .build();
    }

    private sealed interface Step permits Done, Redirect {}

    private record Done(FeedDownload result) implements Step {}

    private record Redirect(URI location) implements Step {}

    private static final class TooLarge extends IOException {}

    @Override
    public FeedDownload download(URI url, String etag, String lastModified) {
        long deadline = System.nanoTime() + limits.total().toNanos();
        URI current = url;
        for (int hop = 0; hop <= limits.maxRedirects(); hop++) {
            String scheme = current.getScheme() == null ? "" : current.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https") || current.getHost() == null) {
                return failed(current, FeedErrorKind.INVALID_URL, null);
            }
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return failed(current, FeedErrorKind.TIMEOUT, null);
            }
            HttpGet get = new HttpGet(current);
            AtomicBoolean expired = new AtomicBoolean();
            // One hard deadline for the whole download: the request is cancelled, whatever it is blocked on.
            ScheduledFuture<?> alarm = timer.schedule(() -> {
                expired.set(true);
                get.cancel();
            }, remaining, TimeUnit.NANOSECONDS);
            try {
                // A literal address never reaches a resolver inside the client: checked here, as every name is again
                // at the connection.
                resolver.resolve(current.getHost());
                get.setHeader(HttpHeaders.ACCEPT, "text/calendar, text/plain;q=0.9, */*;q=0.5");
                if (etag != null) {
                    get.setHeader(HttpHeaders.IF_NONE_MATCH, etag);
                }
                if (lastModified != null) {
                    get.setHeader(HttpHeaders.IF_MODIFIED_SINCE, lastModified);
                }
                // Opened by hand and closed at once, never gracefully: closing gracefully reads the rest of the body.
                ClassicHttpResponse response = client.executeOpen(null, get, null);
                try {
                    Step step = answer(response);
                    switch (step) {
                        case Done done -> {
                            return done.result();
                        }
                        case Redirect redirect -> current = current.resolve(redirect.location());
                    }
                } finally {
                    abort(response);
                }
            } catch (GuardedDnsResolver.BlockedAddressException e) {
                return failed(current, FeedErrorKind.BLOCKED_ADDRESS, e);
            } catch (IOException | RuntimeException e) {
                if (expired.get() || e instanceof InterruptedIOException) {
                    return failed(current, FeedErrorKind.TIMEOUT, e);
                }
                return failed(current, e instanceof TooLarge ? FeedErrorKind.TOO_LARGE : FeedErrorKind.UNREACHABLE, e);
            } finally {
                alarm.cancel(false);
            }
        }
        return failed(current, FeedErrorKind.TOO_MANY_REDIRECTS, null);
    }

    private Step answer(ClassicHttpResponse response) throws IOException {
        int status = response.getCode();
        if (REDIRECTS.contains(status)) {
            Header location = response.getFirstHeader(HttpHeaders.LOCATION);
            try {
                return location == null
                    ? new Done(new FeedDownload.Failed(FeedError.http(status)))
                    : new Redirect(URI.create(location.getValue().strip()));
            } catch (IllegalArgumentException malformed) {
                return new Done(new FeedDownload.Failed(FeedError.http(status)));
            }
        }
        if (status == 304) {
            return new Done(new FeedDownload.NotModified());
        }
        if (status != 200) {
            return new Done(new FeedDownload.Failed(FeedError.http(status)));
        }
        HttpEntity entity = response.getEntity();
        byte[] body = entity == null ? new byte[0] : readCapped(entity.getContent());
        Charset charset = Optional.ofNullable(entity == null ? null : ContentType.parseLenient(entity.getContentType()))
            .map(ContentType::getCharset).orElse(StandardCharsets.UTF_8);
        return new Done(new FeedDownload.Downloaded(new String(body, charset), header(response, HttpHeaders.ETAG),
            header(response, HttpHeaders.LAST_MODIFIED)));
    }

    private byte[] readCapped(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (out.size() + read > limits.maxBytes()) {
                throw new TooLarge();
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static void abort(ClassicHttpResponse response) {
        try {
            if (response instanceof ModalCloseable modal) {
                modal.close(CloseMode.IMMEDIATE);
            } else {
                response.close();
            }
        } catch (IOException ignored) {
            // nothing left to read from
        }
    }

    @Override
    public void close() throws IOException {
        timer.shutdownNow();
        client.close();
    }

    /** A header worth keeping for next time — not one longer than its column. */
    private static String header(ClassicHttpResponse response, String name) {
        Header header = response.getFirstHeader(name);
        return header == null || header.getValue().length() > 200 ? null : header.getValue();
    }

    /** Logged with the host and the code only: the address of a calendar can carry a secret token. */
    private static FeedDownload failed(URI url, FeedErrorKind kind, Exception cause) {
        log.info("Followed calendar on {} not read: {} ({})", url.getHost(), kind, cause == null ? "-" : cause.getClass().getSimpleName());
        return new FeedDownload.Failed(FeedError.of(kind));
    }
}
