package br.com.brforgers.mods.disfabric.utils;// Created 2022-23-10T09:57:24

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class MicroSerialRatelimiter implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MicroSerialRatelimiter.class);

    private static final int RATELIMITED = 429;

    private final ScheduledExecutorService scheduler;
    private final OkHttpClient httpClient;
    private final Queue<Entry> queue = new ConcurrentLinkedQueue<>();

    private final String url;
    private final int paramCount;
    private final Consumer<Request.Builder> defaults;
    private final TimeUnit rateLimitUnit;

    private volatile Entry $live;
    private volatile long until;
    private volatile int remaining;
    private volatile Future<?> future;

    private static final VarHandle $remaining;
    private static final VarHandle live;

    static {
        try {
            var lookup = MethodHandles.lookup();
            // Initialise VarHandles
            live = lookup.findVarHandle(MicroSerialRatelimiter.class, "$live", Entry.class);
            $remaining = lookup.findVarHandle(MicroSerialRatelimiter.class, "remaining", int.class);

            // Ensure LockSupport is initialised.
            lookup.ensureInitialized(LockSupport.class);
        } catch (ReflectiveOperationException roe) {
            throw new ExceptionInInitializerError(roe);
        }
    }


    public MicroSerialRatelimiter(ScheduledExecutorService scheduler, OkHttpClient httpClient, String url, Consumer<Request.Builder> defaults, TimeUnit rateLimitUnit) {
        this.scheduler = Objects.requireNonNullElseGet(scheduler, Executors::newSingleThreadScheduledExecutor);
        this.httpClient = httpClient;
        this.url = url;
        this.paramCount = StringUtils.countParameters(url);
        this.defaults = defaults;
        this.rateLimitUnit = rateLimitUnit;
    }

    private Request.Builder initRequest(String... params) {
        if (params.length != paramCount) {
            throw new IllegalArgumentException("$params.length == " + params.length + "; expected " + paramCount);
        }
        var request = new Request.Builder().url(StringUtils.fillParameters(this.url, params));
        defaults.accept(request);
        return request;
    }

    private CompletableFuture<Response> submitInternal(Request.Builder request) {
        var future = new CompletableFuture<Response>();
        queue.add(new Entry(this, request.build(), future));
        run();
        return future;
    }

    public CompletableFuture<Response> submit(String... params) {
        final var request = initRequest(params);
        return submitInternal(request);
    }

    public CompletableFuture<Response> submit(Consumer<Request.Builder> builder, String... params) {
        final var request = initRequest(params);
        builder.accept(request);
        return submitInternal(request);
    }

    @Override
    public void run() {
        {
            long time = until - System.currentTimeMillis();
            var witness = future;
            logger.debug("Difference {} with delay {} active", time, witness);
            if (time > 0L && remaining == 0) synchronized (this) {
                if (witness == null || witness.isDone()) {
                    logger.debug("Backing off for {}ms for as no requests remain", time);
                    future = scheduler.schedule(this, time, TimeUnit.MILLISECONDS);
                } else {
                    logger.warn("Called while in cooldown.", new Throwable());
                }
                return;
            }
        }

        Entry witness;
        if (!queue.isEmpty() && ((witness = $live) == null || witness.future.isDone())) {
            var next = queue.poll();
            do {
                if (next == null || next.future.isDone()) {
                    logger.warn("{} completed while enqueuing?!", next);
                    next = queue.poll();
                }
                if (live.compareAndExchange(this, witness, next) == next) break;
            } while ((witness = $live) == null || witness.future.isDone());

            if (next != null && !next.future.isDone()) {
                logger.debug("Running {}", next);
                next.run();
            }
        }
    }

    private record Entry(
            MicroSerialRatelimiter $limiter,
            Request request,
            CompletableFuture<Response> future
    ) implements Runnable, Callback {

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            // TODO: Handle SocketTimeoutException specially?
            future.completeExceptionally(e);
            $remaining.getAndAdd($limiter, -1);
            $limiter.scheduler.schedule($limiter, 1, TimeUnit.SECONDS);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            if (response.code() == RATELIMITED) {
                String retry = response.header("retry-after");
                if (retry == null) {
                    onFailure(call, new IOException("[MSRL] Server responded with 429 without Retry-After"));
                    return;
                }
                if (!StringUtils.isNumber(retry)) {
                    onFailure(call, new IOException("[MSRL] No ability to parse " + retry));
                    return;
                }
                logger.debug("Retrying {} in {} seconds", this, retry);
                $limiter.scheduler.schedule(this, Long.parseUnsignedLong(retry), TimeUnit.SECONDS);
            } else {
                future.complete(response);
                String remaining = response.header("x-ratelimit-remaining");
                if (remaining != null) {
                    $limiter.remaining = Integer.parseInt(remaining);
                    logger.debug("Preloaded remaining with {}", remaining);
                }
                String reset = response.header("x-ratelimit-reset");
                if (reset != null) {
                    $limiter.until = $limiter.rateLimitUnit.toMillis(Long.parseUnsignedLong(reset));
                    logger.debug("Preloaded reset with {}", reset);
                }
                $limiter.run();
            }
        }

        public void run() {
            $limiter.httpClient.newCall(request).enqueue(this);
        }
    }
}
