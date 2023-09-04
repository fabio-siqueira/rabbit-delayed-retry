package com.example.delayedretry.rabbit.retry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExponentialDelayHandlerTest {


    @Test
    void shouldGetDelayList() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(10_000)
                .maxRetryAttempts(3)
                .multiplier(2.0)
                .build();

        // when
        var result = target.getDelayList();

        // then
        assertEquals(3, result.size());
    }

    @Test
    void shouldGetLastDelayWhenMessageCountIsGreaterThanMaxAttempts() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(10_000)
                .maxRetryAttempts(6)
                .multiplier(2.0)
                .build();
        // when
        var result = target.getDelay(7);
        // then
        assertEquals("10000", result);
    }

    @Test
    void shouldLimitDelaysSizeToMaxDelay() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(10_000)
                .maxRetryAttempts(6)
                .multiplier(2.0)
                .build();
        // when
        var result = target.getDelayList();

        // then
        assertEquals(5, result.size());
    }

    @Test
    void shouldGetValidDelay() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(10_000)
                .maxRetryAttempts(4)
                .multiplier(3.0)
                .build();

        // then
        assertEquals("1000", target.getDelay(0));
        assertEquals("3000", target.getDelay(1));
        assertEquals("9000", target.getDelay(2));
        assertEquals("10000", target.getDelay(3));
    }

    @Test
    void shouldLimitDelayListSizeToMaxRetryAttempts() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(100_000)
                .maxRetryAttempts(6)
                .multiplier(2.0)
                .build();
        // when
        var result = target.getDelayList();

        // then
        assertEquals(6, result.size());
    }

    @Test
    void shouldGetInitialDelay() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(10_000)
                .maxRetryAttempts(3)
                .multiplier(2.0)
                .build();

        // when
        var result = target.getDelay(0);

        // then
        assertEquals("1000", result);
    }

    @Test
    void shouldGetTrueResponseFromCanRetryMethod() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(10_000)
                .maxRetryAttempts(3)
                .multiplier(2.0)
                .build();

        // then
        assertTrue(target.canRetry(0));
        assertTrue(target.canRetry(1));
        assertTrue(target.canRetry(2));
    }

    @Test
    void shouldGetFalseResponseFromCanRetryMethod() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(10_000)
                .maxRetryAttempts(3)
                .multiplier(2.0)
                .build();

        // then
        assertFalse(target.canRetry(3));
    }

    @Test
    void shouldGetInitialDelayWhenMaxRetryAttemptsIsZero() {
        // given
        ExponentialDelayHandler target = ExponentialDelayHandler.builder()
                .initialDelay(1_000)
                .maxDelay(10_000)
                .maxRetryAttempts(0)
                .multiplier(2.0)
                .build();
        // when
        var result = target.getDelay(0);

        // then
        assertEquals("1000", result);
    }
}