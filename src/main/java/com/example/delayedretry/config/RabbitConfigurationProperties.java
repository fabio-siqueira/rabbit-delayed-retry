package com.example.delayedretry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitConfigurationProperties {

    private int maxConcurrentConsumers;
    private int concurrentConsumers;
    private int prefetchCount;
    private int initialDelay;
    private int maxDelay;
    private int maxRetryAttempts;
    private double multiplier;

    public int getMaxConcurrentConsumers() {
        return maxConcurrentConsumers;
    }

    public RabbitConfigurationProperties setMaxConcurrentConsumers(int maxConcurrentConsumers) {
        this.maxConcurrentConsumers = maxConcurrentConsumers;
        return this;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public RabbitConfigurationProperties setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
        return this;
    }

    public int getPrefetchCount() {
        return prefetchCount;
    }

    public RabbitConfigurationProperties setPrefetchCount(int prefetchCount) {
        this.prefetchCount = prefetchCount;
        return this;
    }

    public int getInitialDelay() {
        return initialDelay;
    }

    public RabbitConfigurationProperties setInitialDelay(int initialDelay) {
        this.initialDelay = initialDelay;
        return this;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public RabbitConfigurationProperties setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
        return this;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public RabbitConfigurationProperties setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
        return this;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public RabbitConfigurationProperties setMultiplier(double multiplier) {
        this.multiplier = multiplier;
        return this;
    }
}
