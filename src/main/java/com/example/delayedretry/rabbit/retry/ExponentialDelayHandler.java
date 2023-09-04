package com.example.delayedretry.rabbit.retry;

import com.rabbitmq.client.RecoveryDelayHandler;

import java.util.ArrayList;
import java.util.List;

public class ExponentialDelayHandler {

    private final long initialDelay;
    private final double multiplier;
    private final int maxRetryAttempts;
    private final long maxDelay;

    public final List<Long> delays;

    private ExponentialDelayHandler(long initialDelay, long maxDelay, double multiplier, int maxRetryAttempts) {
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.multiplier = multiplier;
        this.maxRetryAttempts = maxRetryAttempts;
        this.delays = getDelays();
    }

    private List<Long> getDelays() {
        long delay = initialDelay;
        List<Long> result = new ArrayList<>();

        for (int i = 0; i < maxRetryAttempts && delay <= maxDelay; i++) {
            delay = (long) (initialDelay * Math.pow(multiplier, i));
            result.add(Math.min(delay, maxDelay));
        }
        if (result.isEmpty()) {
            result.add(initialDelay);
        }
        return result;
    }

    protected List<Long> getDelayList() {
        return this.delays;
    }

    /**
     * Returns the delay in milliseconds for the given message count.
     * @param messageCount
     * @return
     */
    public String getDelay(Integer messageCount) {
        RecoveryDelayHandler.ExponentialBackoffDelayHandler exponentialBackoffDelayHandler = new RecoveryDelayHandler.ExponentialBackoffDelayHandler(delays);
        return String.valueOf(exponentialBackoffDelayHandler.getDelay(messageCount));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if the message can be retried.
     * @param retryCount
     * @return
     */
    public boolean canRetry(long retryCount) {
        return retryCount < maxRetryAttempts ;
    }

    public static class Builder {

        private Builder() {
        }

        private long initialDelay;
        private double multiplier;
        private int maxRetryAttempts;
        private long maxDelay;

        public Builder initialDelay(long initialDelay) {
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder maxDelay(long maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        /**
         * Max attempts to retry the message. If the max attempts is reached, the message will be sent to the DLQ.
         * @param maxRetryAttempts
         * @return
         */
        public Builder maxRetryAttempts(int maxRetryAttempts) {
            this.maxRetryAttempts = maxRetryAttempts;
            return this;
        }


        public ExponentialDelayHandler build() {
            return new ExponentialDelayHandler(initialDelay, maxDelay, multiplier, maxRetryAttempts);
        }
    }
}
