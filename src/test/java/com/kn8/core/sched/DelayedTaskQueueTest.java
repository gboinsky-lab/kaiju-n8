// src/test/java/com/kn8/core/sched/DelayedTaskQueueTest.java
package com.kn8.core.sched;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class DelayedTaskQueueTest {

    @Test
    void runsOnlyDueTasksInScheduleOrder() {
        DelayedTaskQueue queue = new DelayedTaskQueue();
        List<String> ran = new ArrayList<>();
        queue.schedule(10, () -> ran.add("a"));
        queue.schedule(12, () -> ran.add("c"));
        queue.schedule(10, () -> ran.add("b"));
        assertEquals(0, queue.runDue(9));
        assertEquals(2, queue.runDue(10));
        assertEquals(List.of("a", "b"), ran);
        assertEquals(1, queue.size());
        assertEquals(1, queue.runDue(20));
        assertEquals(List.of("a", "b", "c"), ran);
        assertEquals(0, queue.size());
    }

    @Test
    void taskScheduledDuringRunWaitsForNextCall() {
        DelayedTaskQueue queue = new DelayedTaskQueue();
        List<String> ran = new ArrayList<>();
        queue.schedule(5, () -> {
            ran.add("first");
            queue.schedule(5, () -> ran.add("second"));
        });
        assertEquals(1, queue.runDue(5));
        assertEquals(List.of("first"), ran);
        assertEquals(1, queue.runDue(5));
        assertEquals(List.of("first", "second"), ran);
    }
}
