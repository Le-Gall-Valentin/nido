package com.nido.api;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * One SMTP server for every test that runs with mail on — the SharedContainers idea applied to mail.
 *
 * <p>On a fixed port, because the port is part of the test configuration: a port that changed per
 * class would give every class a different Spring context, and the context cache would never hit.
 * Started before the first such class, never stopped until the JVM exits.
 */
public final class SharedGreenMail {

    public static final int PORT = 3025;

    private static GreenMail server;

    private SharedGreenMail() {}

    public static synchronized GreenMail server() {
        if (server == null) {
            server = new GreenMail(new ServerSetup(PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        }
        return server;
    }

    /** Starts the server before the test class, so it is up before the context sends anything. */
    public static final class Starter implements BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            server();
        }
    }
}
