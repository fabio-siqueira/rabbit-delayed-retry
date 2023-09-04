package com.example.delayedretry.rabbit;

import com.example.delayedretry.rabbit.retry.ExponentialDelayHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;

import static com.example.delayedretry.rabbit.QueueTopologyTemplate.DEFAULT_DLQ_QUEUE_PREFIX;
import static com.example.delayedretry.rabbit.QueueTopologyTemplate.DEFAULT_RETRY_QUEUE_PREFIX;
import static com.example.delayedretry.rabbit.QueueTopologyTemplate.X_DEAD_LETTER_EXCHANGE;
import static com.example.delayedretry.rabbit.QueueTopologyTemplate.X_DEAD_LETTER_ROUTING_KEY;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.amqp.rabbit.core.RabbitAdmin.DEFAULT_EXCHANGE_NAME;

@ExtendWith(MockitoExtension.class)
class QueueTopologyTemplateTest {

    QueueTopologyTemplate target;

    AmqpTemplate amqpTemplateMock;
    AmqpAdmin amqpAdminMock;

    @BeforeEach
    void setUp() {
        amqpTemplateMock = mock(AmqpTemplate.class);
        amqpAdminMock = mock(AmqpAdmin.class);

        target = new QueueTopologyTemplate(amqpTemplateMock, amqpAdminMock);
    }

    @Test
    void createDelayedRetryMessageRecoverer() {
        // given
        ExponentialDelayHandler delayHandler = ExponentialDelayHandler.builder()
                .initialDelay(1000L)
                .maxDelay(10000L)
                .multiplier(2.0)
                .maxRetryAttempts(5)
                .build();

        // then
        assertNotNull(target.createDelayedRetryInterceptor(delayHandler));
    }

    @Test
    void shouldCreateQueueTopology() {
        // given
        String queueName = "queueName";
        // when
        target.createQueue(queueName);

        // then
        verify(amqpAdminMock, times(1))
                .declareQueue(argThat(queue ->
                        queue.getName().equals(DEFAULT_RETRY_QUEUE_PREFIX.concat(queueName))
                                && queue.getArguments().get(X_DEAD_LETTER_EXCHANGE).equals(DEFAULT_EXCHANGE_NAME)
                                && queue.getArguments().get(X_DEAD_LETTER_ROUTING_KEY).equals(queueName)
                                && queue.getArguments().get("x-queue-type").equals("quorum")
                                && queue.isDurable()
                ));

        verify(amqpAdminMock, times(1))
                .declareQueue(argThat(queue ->
                        queue.getName().equals(queueName)
                                && queue.getArguments().get(X_DEAD_LETTER_EXCHANGE).equals(DEFAULT_EXCHANGE_NAME)
                                && queue.getArguments().get(X_DEAD_LETTER_ROUTING_KEY).equals(DEFAULT_DLQ_QUEUE_PREFIX.concat(queueName))
                                && queue.getArguments().get("x-queue-type").equals("quorum")
                                && queue.isDurable()
                ));

        verify(amqpAdminMock, times(1))
                .declareQueue(argThat(queue ->
                        queue.getName().equals(DEFAULT_DLQ_QUEUE_PREFIX.concat(queueName))
                                && queue.getArguments().get("x-queue-type").equals("quorum")
                                && queue.isDurable()
                ));
    }
}