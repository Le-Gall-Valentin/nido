package com.nido.api.calendar.infrastructure.feed;

import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExternalFeedDownloaderAdapterTest {

    private static final String ICS = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n";

    private HttpServer server;
    private int port;

    /** "feed.test" is this machine; "evil.test" an address of the local network; "rebind.test" this machine too. */
    private final DnsResolver names = new DnsResolver() {
        private final Map<String, String> known = Map.of("feed.test", "127.0.0.1", "evil.test", "10.0.0.1",
            "rebind.test", "127.0.0.1", "metadata.test", "169.254.169.254");

        @Override
        public InetAddress[] resolve(String host) throws UnknownHostException {
            String address = known.get(host);
            if (address == null) {
                throw new UnknownHostException(host);
            }
            return new InetAddress[] {InetAddress.getByName(address)};
        }

        @Override
        public String resolveCanonicalHostname(String host) {
            return host;
        }
    };

    /** Strict, but for this machine — where the test server listens. */
    private final AddressPolicy loopbackOnly = address ->
        address.isLoopbackAddress() || AddressPolicy.standard(() -> false).allows(address);

    private final HttpExternalFeedDownloaderAdapter downloader = new HttpExternalFeedDownloaderAdapter(loopbackOnly, names,
        new HttpExternalFeedDownloaderAdapter.Limits(Duration.ofSeconds(2), Duration.ofSeconds(2), 1024, 3));

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/feed.ics", exchange -> {
            if ("\"v1\"".equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                exchange.sendResponseHeaders(304, -1);
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().add("ETag", "\"v1\"");
            exchange.getResponseHeaders().add("Content-Type", "text/calendar; charset=utf-8");
            send(exchange, 200, ICS.getBytes(StandardCharsets.UTF_8));
        });
        server.createContext("/missing.ics", exchange -> send(exchange, 404, new byte[0]));
        server.createContext("/moved.ics", exchange -> redirect(exchange, "/feed.ics"));
        server.createContext("/loop.ics", exchange -> redirect(exchange, "/loop.ics"));
        server.createContext("/to-evil.ics", exchange -> redirect(exchange, "http://evil.test/x.ics"));
        server.createContext("/to-metadata.ics", exchange -> redirect(exchange, "http://169.254.169.254/latest/meta-data/"));
        server.createContext("/big.ics", exchange -> send(exchange, 200, new byte[2048]));
        server.createContext("/bomb.ics", exchange -> {
            ByteArrayOutputStream zipped = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(zipped)) {
                gzip.write(new byte[64 * 1024]);
            }
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            send(exchange, 200, zipped.toByteArray());
        });
        server.createContext("/slow.ics", exchange -> {
            try {
                Thread.sleep(4_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            send(exchange, 200, ICS.getBytes(StandardCharsets.UTF_8));
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void aCalendarIsDownloadedWithItsEtag() {
        FeedDownload download = downloader.download(url("feed.test", "/feed.ics"), null, null);
        assertThat(download).isEqualTo(new FeedDownload.Downloaded(ICS, "\"v1\"", null));
    }

    @Test
    void anUnchangedCalendarIsNotDownloadedAgain() {
        assertThat(downloader.download(url("feed.test", "/feed.ics"), "\"v1\"", null)).isEqualTo(new FeedDownload.NotModified());
    }

    @Test
    void anErrorStatusIsNamed() {
        assertThat(downloader.download(url("feed.test", "/missing.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.http(404)));
    }

    @Test
    void aRedirectIsFollowed() {
        assertThat(downloader.download(url("feed.test", "/moved.ics"), null, null)).isInstanceOf(FeedDownload.Downloaded.class);
    }

    @Test
    void threeRedirectsAtMost() {
        assertThat(downloader.download(url("feed.test", "/loop.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TOO_MANY_REDIRECTS)));
    }

    @Test
    void a_redirect_to_a_blocked_address_is_refused() {
        assertThat(downloader.download(url("feed.test", "/to-evil.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.BLOCKED_ADDRESS)));
        assertThat(downloader.download(url("feed.test", "/to-metadata.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.BLOCKED_ADDRESS)));
    }

    @Test
    void aNameGivingABlockedAddressIsRefusedBeforeAnyConnection() {
        HttpExternalFeedDownloaderAdapter strict = new HttpExternalFeedDownloaderAdapter(AddressPolicy.standard(() -> false),
            names, HttpExternalFeedDownloaderAdapter.Limits.standard());
        assertThat(strict.download(url("rebind.test", "/feed.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.BLOCKED_ADDRESS)));
        assertThat(strict.download(URI.create("http://127.0.0.1:" + port + "/feed.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.BLOCKED_ADDRESS)));
    }

    @Test
    void aFileLargerThanTheLimitIsRefused() {
        assertThat(downloader.download(url("feed.test", "/big.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TOO_LARGE)));
    }

    @Test
    void aCompressedBombIsCountedOnceUnpacked() {
        assertThat(downloader.download(url("feed.test", "/bomb.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TOO_LARGE)));
    }

    @Test
    void aServerTooSlowIsGivenUpOn() {
        assertThat(downloader.download(url("feed.test", "/slow.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TIMEOUT)));
    }

    @Test
    void anUnknownNameIsUnreachable() {
        assertThat(downloader.download(url("nowhere.test", "/feed.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.UNREACHABLE)));
    }

    private URI url(String host, String path) {
        return URI.create("http://" + host + ":" + port + path);
    }

    private static void redirect(com.sun.net.httpserver.HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static void send(com.sun.net.httpserver.HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
        exchange.close();
    }
}
