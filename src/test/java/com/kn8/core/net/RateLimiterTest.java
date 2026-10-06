// src/test/java/com/kn8/core/net/RateLimiterTest.java
package com.kn8.core.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    @Test
    void burstIsAllowedThenExcessIsDropped() {
        RateLimiter<String> limiter = new RateLimiter<>();
        int accepted = 0;
        for (int i = 0; i < 100; i++) {
            if (limiter.tryAcquire("dev1", "ping", 20, 40, 0)) {
                accepted++;
            }
        }
        assertEquals(40, accepted);
        assertEquals(40, limiter.stats("dev1").get("ping").accepted());
        assertEquals(60, limiter.stats("dev1").get("ping").dropped());
    }

    @Test
    void bucketRefillsOverTime() {
        RateLimiter<String> limiter = new RateLimiter<>();
        for (int i = 0; i < 40; i++) {
            limiter.tryAcquire("dev1", "ping", 20, 40, 0);
        }
        assertFalse(limiter.tryAcquire("dev1", "ping", 20, 40, 0));
        // 20 por segundo = 1 por tick.
        assertTrue(limiter.tryAcquire("dev1", "ping", 20, 40, 1));
        assertFalse(limiter.tryAcquire("dev1", "ping", 20, 40, 1));
    }

    @Test
    void ownersAndChannelsAreIndependent() {
        RateLimiter<String> limiter = new RateLimiter<>();
        assertTrue(limiter.tryAcquire("dev1", "a", 1, 1, 0));
        assertFalse(limiter.tryAcquire("dev1", "a", 1, 1, 0));
        assertTrue(limiter.tryAcquire("dev1", "b", 1, 1, 0));
        assertTrue(limiter.tryAcquire("dev2", "a", 1, 1, 0));
    }

    @Test
    void forgetClearsAnOwner() {
        RateLimiter<String> limiter = new RateLimiter<>();
        limiter.tryAcquire("dev1", "a", 1, 1, 0);
        limiter.forget("dev1");
        assertTrue(limiter.stats("dev1").isEmpty());
        assertTrue(limiter.tryAcquire("dev1", "a", 1, 1, 0));
    }

    @Test
    void invalidBucketIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new TokenBucket(1, 0, 0));
    }
}
