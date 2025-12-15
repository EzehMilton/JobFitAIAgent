package com.milton.agent.util;

import org.slf4j.Logger;

/**
 * Lightweight helper to log duration of a scoped operation.
 */
public final class TimedOperation implements AutoCloseable {

    private final Logger log;
    private final String action;
    private final long startedAt;

    private TimedOperation(Logger log, String action) {
        this.log = log;
        this.action = action;
        this.startedAt = System.currentTimeMillis();
        this.log.info("{} started", action);
    }

    public static TimedOperation start(Logger log, String action) {
        return new TimedOperation(log, action);
    }

    @Override
    public void close() {
        long durationMs = System.currentTimeMillis() - startedAt;
        log.info("{} completed in {} ms", action, durationMs);
    }
}
