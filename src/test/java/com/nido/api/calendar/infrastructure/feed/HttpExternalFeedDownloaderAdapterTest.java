package com.nido.api.calendar.infrastructure.feed;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExternalFeedDownloaderAdapterTest {

    private static final String ICS = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n";

    private final java.util.List<HttpExternalFeedDownloaderAdapter> adapters = new java.util.ArrayList<>();
    private final java.util.List<AutoCloseable> held = new java.util.ArrayList<>();
    private final AtomicLong written = new AtomicLong();
    private final AtomicInteger requests = new AtomicInteger();
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool();
    private java.net.ServerSocket drip;
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
        address.isLoopbackAddress() || AddressPolicy.standard(() -> false, AddressPolicyTest.NOTHING_OF_NIDO).allows(address);

    private final HttpExternalFeedDownloaderAdapter downloader = adapter(loopbackOnly, names,
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
            requests.incrementAndGet();
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
            pause(6_000);
            exchange.close();
        });
        server.createContext("/late1.ics", exchange -> {
            pause(1_500);
            redirect(exchange, "/late2.ics");
        });
        server.createContext("/late2.ics", exchange -> {
            pause(1_500);
            redirect(exchange, "/late3.ics");
        });
        server.createContext("/late3.ics", exchange -> {
            pause(1_500);
            redirect(exchange, "/late1.ics");
        });
        server.createContext("/bad-location.ics", exchange -> redirect(exchange, "http://[bad"));
        server.createContext("/lie200.ics", exchange -> lie(exchange, 200));
        server.createContext("/lie404.ics", exchange -> lie(exchange, 404));
        server.createContext("/endless.ics", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            byte[] chunk = new byte[8192];
            try (OutputStream out = exchange.getResponseBody()) {
                for (int i = 0; i < 1_000_000; i++) {
                    out.write(chunk);
                    written.addAndGet(chunk.length);
                }
            } catch (IOException clientGone) {
                // the client aborted, as it should
            }
        });
        server.createContext("/endless-deflate.ics", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(new byte[] {0x1f, (byte) 0x8b, 8, 0, 0, 0, 0, 0, 0, 0});
                out.flush();
                for (int i = 0; i < 40; i++) {
                    out.write(new byte[] {0, 0, 0, (byte) 0xff, (byte) 0xff}); // empty stored blocks: no byte ever comes out
                    out.flush();
                    pause(200);
                }
            } catch (IOException clientGone) {
                // aborted
            }
        });
        server.setExecutor(executor);
        server.start();
        drip = new java.net.ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));
        Thread.ofPlatform().daemon().start(this::dripHeaders);
        port = server.getAddress().getPort();
    }

    private HttpExternalFeedDownloaderAdapter adapter(AddressPolicy policy, DnsResolver dns,
                                                      HttpExternalFeedDownloaderAdapter.Limits limits) {
        HttpExternalFeedDownloaderAdapter created = new HttpExternalFeedDownloaderAdapter(policy, dns, limits);
        adapters.add(created);
        return created;
    }

    /** A listener whose queue of waiting connections is full: any further connection to its port hangs. */
    private int deadPort() throws IOException {
        java.net.ServerSocket full = new java.net.ServerSocket(0, 1);
        held.add(full);
        for (int i = 0; i < 6; i++) {
            java.net.Socket filler = new java.net.Socket();
            held.add(filler);
            try {
                filler.connect(new InetSocketAddress("127.0.0.1", full.getLocalPort()), 300);
            } catch (java.net.SocketTimeoutException hangs) {
                break;
            }
        }
        return full.getLocalPort();
    }

    private static DnsResolver deadNames() {
        return new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                if (host.equals("feed.test")) {
                    return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
                }
                return new InetAddress[] {InetAddress.getByName("127.0.0.2"), InetAddress.getByName("127.0.0.3"),
                    InetAddress.getByName("127.0.0.4")};
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };
    }

    @AfterEach
    void stop() throws Exception {
        for (HttpExternalFeedDownloaderAdapter created : adapters) {
            created.close();
        }
        for (AutoCloseable open : held) {
            open.close();
        }
        server.stop(0);
        executor.shutdownNow();
        drip.close();
    }

    @Test
    void aCalendarIsDownloadedWithItsEtag() {
        FeedDownload download = downloader.download(url("feed.test", "/feed.ics"), null, null);
        assertThat(download).isEqualTo(new FeedDownload.Downloaded(ICS, "\"v1\"", null));
    }

    @Test
    void aHeaderLongerThanItsColumnIsNotKept() {
        String etag = "\"" + "e".repeat(150) + "\"";
        server.createContext("/long-headers.ics", exchange -> {
            exchange.getResponseHeaders().add("ETag", etag);
            exchange.getResponseHeaders().add("Last-Modified", "Wed, 14 Oct 2026 07:28:00 GMT" + " ".repeat(71) + "x");
            send(exchange, 200, ICS.getBytes(StandardCharsets.UTF_8));
        });
        // The ETag fits its 200 characters; a Last-Modified of 101 does not fit its 100.
        assertThat(downloader.download(url("feed.test", "/long-headers.ics"), null, null))
            .isEqualTo(new FeedDownload.Downloaded(ICS, etag, null));
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
        HttpExternalFeedDownloaderAdapter strict = adapter(AddressPolicy.standard(() -> false, AddressPolicyTest.NOTHING_OF_NIDO),
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
        assertTimeoutWithin("/slow.ics");
    }

    @Test
    void aServerDrippingItsHeadersIsGivenUpOnAtTheDeadline() {
        long start = System.nanoTime();
        FeedDownload result = downloader.download(URI.create("http://feed.test:" + drip.getLocalPort() + "/drip.ics"), null, null);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(result).isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TIMEOUT)));
        assertThat(millis).as("elapsed").isLessThan(3_000);
    }

    @Test
    void redirectsEachAnsweringLateAreGivenUpOnAtTheDeadline() {
        assertTimeoutWithin("/late1.ics");
    }

    @Test
    void endlessEmptyDeflateBlocksAreGivenUpOnAtTheDeadline() {
        assertTimeoutWithin("/endless-deflate.ics");
    }

    @Test
    void aFailureIsLoggedWithItsHostAndCodeAndNeverItsAddress() {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpExternalFeedDownloaderAdapter.class);
        ListAppender<ILoggingEvent> logged = new ListAppender<>();
        logged.start();
        logger.addAppender(logged);
        try {
            downloader.download(url("feed.test", "/missing.ics?token=s3cr3t-of-the-address"), null, null);
        } finally {
            logger.detachAppender(logged);
        }
        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage).singleElement().asString()
            .contains("feed.test", "HTTP_404").doesNotContain("s3cr3t", "missing.ics");
    }

    @Test
    void addressesThatNeverConnectAreTriedUntilTheDeadlineAndNoLonger() throws IOException {
        int dead = deadPort();
        HttpExternalFeedDownloaderAdapter blackhole = adapter(address -> true, deadNames(),
            new HttpExternalFeedDownloaderAdapter.Limits(Duration.ofSeconds(1), Duration.ofSeconds(2), 1024, 3));
        long start = System.nanoTime();
        FeedDownload result = blackhole.download(URI.create("http://dead.test:" + dead + "/feed.ics"), null, null);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(result).isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TIMEOUT)));
        // A second address is tried, cut by the deadline: the download does not give up a second early.
        assertThat(millis).as("elapsed").isBetween(1_500L, 3_000L);
    }

    @Test
    void aHopStartingLateDoesNotTryMoreAddressesThanTheTimeLeft() throws IOException {
        int dead = deadPort();
        server.createContext("/late-then-dead.ics", exchange -> {
            pause(2_500);
            redirect(exchange, "http://dead.test:" + dead + "/x.ics");
        });
        HttpExternalFeedDownloaderAdapter late = adapter(address -> true, deadNames(),
            new HttpExternalFeedDownloaderAdapter.Limits(Duration.ofSeconds(1), Duration.ofSeconds(3), 1024, 3));
        long start = System.nanoTime();
        FeedDownload result = late.download(url("feed.test", "/late-then-dead.ics"), null, null);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(result).isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TIMEOUT)));
        assertThat(millis).as("elapsed").isLessThan(3_800);
    }

    @Test
    void aClosedDownloaderAnswersFailedInsteadOfThrowing() throws Exception {
        HttpExternalFeedDownloaderAdapter closed = adapter(loopbackOnly, names,
            new HttpExternalFeedDownloaderAdapter.Limits(Duration.ofSeconds(2), Duration.ofSeconds(2), 1024, 3));
        closed.close();
        assertThat(closed.download(url("feed.test", "/feed.ics"), null, null)).isInstanceOf(FeedDownload.Failed.class);
    }

    @Test
    void aLyingContentLengthIsNotReadToItsEnd() {
        assertThat(downloader.download(url("feed.test", "/lie200.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TOO_LARGE)));
        assertThat(downloader.download(url("feed.test", "/lie404.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.http(404)));
        assertThat(written.get()).as("bytes the server managed to write").isLessThan(10_000_000L);
    }

    @Test
    void anEndlessChunkedBodyIsNotReadToItsEnd() {
        long start = System.nanoTime();
        assertThat(downloader.download(url("feed.test", "/endless.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TOO_LARGE)));
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(3_000);
        assertThat(written.get()).isLessThan(10_000_000L);
    }

    @Test
    void aMalformedLocationIsAnHttpError() {
        assertThat(downloader.download(url("feed.test", "/bad-location.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.http(302)));
    }

    @Test
    void theNameIsCheckedAgainAtTheConnection() {
        // The first resolution (the adapter's own check) is allowed, the second (the connection's) is not.
        AtomicInteger calls = new AtomicInteger();
        DnsResolver rebinding = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                return new InetAddress[] {InetAddress.getByName(calls.getAndIncrement() == 0 ? "8.8.8.8" : "10.0.0.1")};
            }

            @Override
            public String resolveCanonicalHostname(String host) {
                return host;
            }
        };
        requests.set(0);
        HttpExternalFeedDownloaderAdapter victim = adapter(AddressPolicy.standard(() -> false, AddressPolicyTest.NOTHING_OF_NIDO),
            rebinding, HttpExternalFeedDownloaderAdapter.Limits.standard());
        assertThat(victim.download(url("rebind.test", "/feed.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.BLOCKED_ADDRESS)));
        assertThat(calls.get()).as("resolutions").isGreaterThanOrEqualTo(2);
        assertThat(requests.get()).as("requests reaching the server").isZero();
    }

    private void assertTimeoutWithin(String path) {
        long start = System.nanoTime();
        FeedDownload result = downloader.download(url("feed.test", path), null, null);
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(result).isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TIMEOUT)));
        assertThat(millis).as("elapsed").isLessThan(3_000);
    }

    @Test
    void anUnknownNameIsUnreachable() {
        assertThat(downloader.download(url("nowhere.test", "/feed.ics"), null, null))
            .isEqualTo(new FeedDownload.Failed(FeedError.of(FeedErrorKind.UNREACHABLE)));
    }

    private URI url(String host, String path) {
        return URI.create("http://" + host + ":" + port + path);
    }

    /** Answers one byte of its status line every half second: no read ever times out, the answer never ends. */
    private void dripHeaders() {
        while (!drip.isClosed()) {
            try {
                java.net.Socket socket = drip.accept();
                Thread.ofPlatform().daemon().start(() -> {
                    try (socket) {
                        socket.getInputStream().read(new byte[4096]);
                        OutputStream out = socket.getOutputStream();
                        for (byte b : "HTTP/1.1 200 OK\r\nX-Slow: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes(StandardCharsets.US_ASCII)) {
                            out.write(b);
                            out.flush();
                            pause(500);
                        }
                    } catch (IOException clientGone) {
                        // gave up, as it should
                    }
                });
            } catch (IOException closed) {
                return;
            }
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Announces two gigabytes and writes them for as long as the client reads. */
    private void lie(com.sun.net.httpserver.HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, 2_000_000_000L);
        byte[] chunk = new byte[8192];
        try (OutputStream out = exchange.getResponseBody()) {
            for (int i = 0; i < 250_000; i++) {
                out.write(chunk);
                written.addAndGet(chunk.length);
            }
        } catch (IOException clientGone) {
            // the client aborted, as it should
        }
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
