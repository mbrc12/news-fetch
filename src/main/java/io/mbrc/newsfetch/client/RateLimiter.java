package io.mbrc.newsfetch.client;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;

@Slf4j
public class RateLimiter {

    private final Deque<Instant> eventDeque;
    private final Duration duration;
    private final long ticks;

    // Rate limit number of ticks to ticks / duration
    public RateLimiter(long ticks, Duration duration) {
        this.eventDeque = new LinkedList<>();
        this.ticks = ticks;
        this.duration = duration;
        if (ticks == 0 || duration.isZero()) {
            throw new IllegalArgumentException("Illegal values in RateLimiter constants");
        }
    }

    private boolean withinRange(Instant t_1, Instant t_2) {
        return Duration.between(t_1, t_2).compareTo(this.duration) <= 0;
    }

    // Is this tick valid (not rate limited), and if yes, add to event list.
    synchronized public boolean tick() {
        Instant now = Instant.now();

        while (!eventDeque.isEmpty() && !withinRange(eventDeque.peekFirst(), now)) {
            eventDeque.removeFirst();
        }
        // We don't yet have `ticks` many events in the duration
        if (eventDeque.size() < this.ticks) {
            eventDeque.addLast(now);
            log.info(String.format("Event: %s. Allowed.", now.toString()));
            return true;
        }

        log.info(String.format("Event %s. Restricted.", now.toString()));
        return false;
    }

    public void rateLimited(Runnable runnable) {
        if (tick()) {
            runnable.run();
        }
    }
}
