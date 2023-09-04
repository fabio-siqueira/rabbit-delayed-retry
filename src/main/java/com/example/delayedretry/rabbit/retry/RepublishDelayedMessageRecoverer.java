package com.example.delayedretry.rabbit.retry;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

import static com.example.delayedretry.rabbit.QueueTopologyTemplate.DEFAULT_DLQ_QUEUE_PREFIX;
import static com.example.delayedretry.rabbit.QueueTopologyTemplate.DEFAULT_RETRY_QUEUE_PREFIX;
import static org.springframework.amqp.rabbit.core.RabbitAdmin.DEFAULT_EXCHANGE_NAME;

/**
 * A {@link MessageRecoverer} that republishes messages to a retry queue with a delay or a dead letter queue.
 * If the message has a "x-death" header with a "count" greater than the maxAttempts, it will be republished to the dlq exchange.
 * If the message has a "x-death" header with a "count" less than the maxAttempts, it will be republished to the retry exchange with a delay.
 */
public class RepublishDelayedMessageRecoverer implements MessageRecoverer {

    private static final Logger log = LoggerFactory.getLogger(RepublishDelayedMessageRecoverer.class);

    private static final Expression RETRY_ROUTING_KEY_EXPRESSION = new SpelExpressionParser().parseExpression("\"" + DEFAULT_RETRY_QUEUE_PREFIX + "\" + messageProperties.consumerQueue");
    private static final Expression DLQ_ROUTING_KEY_EXPRESSION = new SpelExpressionParser().parseExpression("\"" + DEFAULT_DLQ_QUEUE_PREFIX + "\" + messageProperties.consumerQueue");
    private static final LiteralExpression DEFAULT_EXCHANGE_EXPRESSION = new LiteralExpression(DEFAULT_EXCHANGE_NAME);

    private final MessageRecoverer retryMessageRecoverer;
    private final MessageRecoverer errorMessageRecoverer;
    private final ExponentialDelayHandler delayHandler;

    public RepublishDelayedMessageRecoverer(@Nonnull AmqpTemplate amqpTemplate, @Nonnull ExponentialDelayHandler delayHandler) {
        Assert.notNull(amqpTemplate, "'amqpTemplate' cannot be null");
        this.retryMessageRecoverer = new RepublishMessageRecoverer(amqpTemplate, DEFAULT_EXCHANGE_EXPRESSION, RETRY_ROUTING_KEY_EXPRESSION);
        this.errorMessageRecoverer = new RepublishMessageRecoverer(amqpTemplate, DEFAULT_EXCHANGE_EXPRESSION, DLQ_ROUTING_KEY_EXPRESSION);
        this.delayHandler = delayHandler;
    }

    public RepublishDelayedMessageRecoverer(MessageRecoverer retryMessageRecoverer, MessageRecoverer errorMessageRecoverer, ExponentialDelayHandler delayHandler) {
        Assert.notNull(retryMessageRecoverer, "'retryMessageRecoverer' cannot be null");
        Assert.notNull(errorMessageRecoverer, "'errorMessageRecoverer' cannot be null");
        Assert.notNull(delayHandler, "'delayHandler' cannot be null");

        this.retryMessageRecoverer = retryMessageRecoverer;
        this.errorMessageRecoverer = errorMessageRecoverer;
        this.delayHandler = delayHandler;
    }

    /**
     * Recovers the message by republishing it to the exchange it was originally sent to.
     * If the message was marked as mandatory but cannot be routed, it will be republished to the default exchange.
     * If the message has a "x-death" header, it will be republished to the default exchange.
     * If the message has a "x-death" header with a "count" greater than the maxAttempts, it will be republished to the dlq exchange.
     * If the message has a "x-death" header with a "count" less than the maxAttempts, it will be republished to the retry exchange with a delay.
     *
     * @param message The message.
     * @param cause   The cause.
     */
    @Override
    public void recover(Message message, Throwable cause) {
        List<Map<String, ?>> xDeathHeader = message.getMessageProperties().getXDeathHeader();

        long retryCount = xDeathHeader == null ? 0L : (Long) xDeathHeader.get(0).get("count");

        if (delayHandler.canRetry(retryCount)) {
            String delay = delayHandler.getDelay((int) retryCount);
            message.getMessageProperties().setExpiration(delay);

            log.debug("The failed message will be sent to retry queue with delay={}ms", delay);
            retryMessageRecoverer.recover(message, cause);

        } else {
            log.debug("Retries count exceed max attempts, sending failed message to dead letter queue");
            errorMessageRecoverer.recover(message, cause);
        }
    }
}
