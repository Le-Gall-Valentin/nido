package com.nido.api.infrastructure.ratelimit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * The Lettuce commands bucket4j writes its buckets through, re-resolved whenever the connection
 * factory hands out a different client.
 *
 * <p>bucket4j takes one connection and keeps it, which is the right shape for it: the buckets are
 * read and written on every limited request, and borrowing a connection per call would put a pool
 * checkout in front of each one. What it cannot know is that the connection it was given belongs to a
 * {@link LettuceConnectionFactory} with a lifecycle of its own — {@code stop()} disposes the native
 * client, {@code start()} builds a different one — so a connection captured once is dead for the rest
 * of the process the first time that happens. Every limited route then answers 500 with
 * {@code RedisException: Connection is closed}, and nothing short of a restart recovers.
 *
 * <p>What is handed to bucket4j is therefore a proxy: each call resolves the live connection behind
 * two field reads — the client is still the one the connection came from, and the connection is still
 * open — and reuses it for as long as both hold, which is all of the time in a running application.
 *
 * <p>A reflective proxy rather than an implementation of the interface, because
 * {@code RedisAsyncCommands} declares several hundred methods of which this needs three, and because
 * bucket4j's own adapter is what should keep deciding how those three are called.
 */
class RebindingLettuceCommands implements InvocationHandler, DisposableBean {

    /** Keys are strings; bucket4j owns the values, which it reads and writes as raw bytes. */
    private static final RedisCodec<String, byte[]> CODEC =
        RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE);

    private final LettuceConnectionFactory connectionFactory;

    /** Written only under the monitor, read without it — a record, so it is never half-updated. */
    private volatile Binding binding;

    RebindingLettuceCommands(LettuceConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private record Binding(RedisClient client, StatefulRedisConnection<String, byte[]> connection) {

        boolean stillServes(RedisClient current) {
            return client == current && connection.isOpen();
        }
    }

    @SuppressWarnings("unchecked")
    RedisAsyncCommands<String, byte[]> asCommands() {
        return (RedisAsyncCommands<String, byte[]>) Proxy.newProxyInstance(
            RedisAsyncCommands.class.getClassLoader(), new Class<?>[]{RedisAsyncCommands.class}, this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(commands(), args);
        } catch (InvocationTargetException e) {
            // The caller asked Redis a question; it must see Redis's answer, not the reflection.
            throw e.getCause();
        }
    }

    private RedisAsyncCommands<String, byte[]> commands() {
        RedisClient current = currentClient();
        Binding existing = binding;
        if (existing != null && existing.stillServes(current)) {
            return existing.connection().async();
        }
        return rebind(current).async();
    }

    private synchronized StatefulRedisConnection<String, byte[]> rebind(RedisClient current) {
        Binding existing = binding;
        if (existing != null && existing.stillServes(current)) {
            return existing.connection();
        }
        if (existing != null) {
            closeQuietly(existing.connection());
        }
        StatefulRedisConnection<String, byte[]> fresh = current.connect(CODEC);
        binding = new Binding(current, fresh);
        return fresh;
    }

    private RedisClient currentClient() {
        if (connectionFactory.getRequiredNativeClient() instanceof RedisClient client) {
            return client;
        }
        throw new IllegalStateException("Expected a Lettuce standalone RedisClient, got: "
            + connectionFactory.getRequiredNativeClient());
    }

    @Override
    public void destroy() {
        Binding existing = binding;
        binding = null;
        if (existing != null) {
            closeQuietly(existing.connection());
        }
    }

    /**
     * A connection is closed here because it is being replaced or the application is going down; in
     * both cases what happens to the old one decides nothing.
     */
    private static void closeQuietly(StatefulRedisConnection<String, byte[]> connection) {
        try {
            connection.close();
        } catch (RuntimeException ignored) {
            // Already gone — which is the state being asked for.
        }
    }
}
