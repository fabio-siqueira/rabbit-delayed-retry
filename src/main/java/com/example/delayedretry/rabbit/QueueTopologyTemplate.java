package com.example.delayedretry.rabbit;

import com.example.delayedretry.rabbit.retry.ExponentialDelayHandler;
import com.example.delayedretry.rabbit.retry.RepublishDelayedMessageRecoverer;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.stereotype.Component;

import static org.springframework.amqp.core.QueueBuilder.durable;
import static org.springframework.amqp.rabbit.core.RabbitAdmin.DEFAULT_EXCHANGE_NAME;

@Component
public class QueueTopologyTemplate {

    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String DEFAULT_DLQ_QUEUE_PREFIX = "dlq.";
    public static final String DEFAULT_RETRY_QUEUE_PREFIX = "retry.";

    private final AmqpTemplate amqpTemplate;
    private final AmqpAdmin amqpAdmin;

    public QueueTopologyTemplate(AmqpTemplate amqpTemplate, AmqpAdmin amqpAdmin) {
        this.amqpTemplate = amqpTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    /**
     * Creates a retry interceptor with a delayed message recoverer
     *
     * @param delayHandler
     * @return
     */
    public RetryOperationsInterceptor createDelayedRetryInterceptor(ExponentialDelayHandler delayHandler) {
        MessageRecoverer recoverer = new RepublishDelayedMessageRecoverer(amqpTemplate, delayHandler);
        return RetryInterceptorBuilder.stateless()
                .recoverer(recoverer)
                .maxAttempts(1)
                .build();
    }


    /**
     * Creates a queue with dead lettered retry and dead lettered queues
     *
     * @param queueName
     */
    public void createQueue(String queueName) {
        // declare dead lettered retry queue
        declareDeadLetteredQueue(amqpAdmin, DEFAULT_RETRY_QUEUE_PREFIX.concat(queueName), queueName);

        // declare dead lettered queue
        declareDeadLetteredQueue(amqpAdmin, queueName, DEFAULT_DLQ_QUEUE_PREFIX.concat(queueName));

        // declare dlq
        amqpAdmin.declareQueue(durable(DEFAULT_DLQ_QUEUE_PREFIX.concat(queueName))
                .quorum()
                .build());
    }

    private static void declareDeadLetteredQueue(AmqpAdmin amqpAdmin, String queueName, String routingKey) {
        amqpAdmin.declareQueue(durable(queueName)
                .withArgument(X_DEAD_LETTER_EXCHANGE, DEFAULT_EXCHANGE_NAME)
                .withArgument(X_DEAD_LETTER_ROUTING_KEY, routingKey)
                .quorum()
                .build());
    }
}
