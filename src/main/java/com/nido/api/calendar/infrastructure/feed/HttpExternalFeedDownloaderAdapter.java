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
import org.apache.hc.core5.util.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Downloads a followed calendar: http(s) only, every address checked through {@link GuardedDnsResolver} — redirects
 * followed by hand, each one checked again —, 5 s to connect and 15 s in all, 5 MB at most counted as it arrives and
 * once unpacked, no cookie, no credential. An error is a code, never a word of what the server answered.
 */
public final class HttpExternalFeedDownloaderAdapter implements ExternalFeedDownloader {

    public record Limits(Duration connect, Duration total, int maxBytes, int maxRedirects) {
        public static Limits standard() {
            return new Limits(Duration.ofSeconds(5), Duration.ofSeconds(15), 5 * 1024 * 1024, 3);
        }
    }

    private static final Set<Integer> REDIRECTS = Set.of(301, 302, 303, 307, 308);

    private final GuardedDnsResolver resolver;
    private final Limits limits;
    private final CloseableHttpClient client;

    public HttpExternalFeedDownloaderAdapter(AddressPolicy policy, DnsResolver dns, Limits limits) {
        this.resolver = new GuardedDnsResolver(dns, policy);
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
            .disableCookieManagement()
            .disableAuthCaching()
            .setUserAgent("nido/" + NidoVersion.current())
            .build();
    }

    private sealed interface Step permits Done, Redirect {}

    private record Done(FeedDownload result) implements Step {}

    private record Redirect(URI location) implements Step {}

    private static final class TooLarge extends IOException {}

    private static final class DeadlineExceeded extends IOException {}

    @Override
    public FeedDownload download(URI url, String etag, String lastModified) {
        long deadline = System.nanoTime() + limits.total().toNanos();
        URI current = url;
        for (int hop = 0; hop <= limits.maxRedirects(); hop++) {
            String scheme = current.getScheme() == null ? "" : current.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https") || current.getHost() == null) {
                return failed(FeedErrorKind.INVALID_URL);
            }
            try {
                // A literal address never reaches a resolver inside the client: checked here, as every name is again
                // at the connection.
                resolver.resolve(current.getHost());
                HttpGet get = new HttpGet(current);
                get.setHeader(HttpHeaders.ACCEPT, "text/calendar, text/plain;q=0.9, */*;q=0.5");
                if (etag != null) {
                    get.setHeader(HttpHeaders.IF_NONE_MATCH, etag);
                }
                if (lastModified != null) {
                    get.setHeader(HttpHeaders.IF_MODIFIED_SINCE, lastModified);
                }
                Step step = client.execute(get, response -> answer(response, deadline));
                switch (step) {
                    case Done done -> {
                        return done.result();
                    }
                    case Redirect redirect -> current = current.resolve(redirect.location());
                }
            } catch (GuardedDnsResolver.BlockedAddressException e) {
                return failed(FeedErrorKind.BLOCKED_ADDRESS);
            } catch (TooLarge e) {
                return failed(FeedErrorKind.TOO_LARGE);
            } catch (DeadlineExceeded | InterruptedIOException e) {
                return failed(FeedErrorKind.TIMEOUT);
            } catch (UnknownHostException e) {
                return failed(FeedErrorKind.UNREACHABLE);
            } catch (IOException | IllegalArgumentException e) {
                return failed(FeedErrorKind.UNREACHABLE);
            }
        }
        return failed(FeedErrorKind.TOO_MANY_REDIRECTS);
    }

    private Step answer(ClassicHttpResponse response, long deadline) throws IOException {
        int status = response.getCode();
        if (REDIRECTS.contains(status)) {
            Header location = response.getFirstHeader(HttpHeaders.LOCATION);
            return location == null
                ? new Done(new FeedDownload.Failed(FeedError.http(status)))
                : new Redirect(URI.create(location.getValue().strip()));
        }
        if (status == 304) {
            return new Done(new FeedDownload.NotModified());
        }
        if (status != 200) {
            return new Done(new FeedDownload.Failed(FeedError.http(status)));
        }
        HttpEntity entity = response.getEntity();
        byte[] body = entity == null ? new byte[0] : readCapped(entity.getContent(), deadline);
        Charset charset = Optional.ofNullable(entity == null ? null : ContentType.parseLenient(entity.getContentType()))
            .map(ContentType::getCharset).orElse(StandardCharsets.UTF_8);
        return new Done(new FeedDownload.Downloaded(new String(body, charset), header(response, HttpHeaders.ETAG),
            header(response, HttpHeaders.LAST_MODIFIED)));
    }

    private byte[] readCapped(InputStream in, long deadline) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (System.nanoTime() > deadline) {
                throw new DeadlineExceeded();
            }
            if (out.size() + read > limits.maxBytes()) {
                throw new TooLarge();
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /** A header worth keeping for next time — not one longer than its column. */
    private static String header(ClassicHttpResponse response, String name) {
        Header header = response.getFirstHeader(name);
        return header == null || header.getValue().length() > 200 ? null : header.getValue();
    }

    private static FeedDownload failed(FeedErrorKind kind) {
        return new FeedDownload.Failed(FeedError.of(kind));
    }
}
