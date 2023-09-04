package com.example.delayedretry.config;

import com.example.delayedretry.rabbit.retry.ExponentialDelayHandler;
import com.example.delayedretry.rabbit.QueueTopologyTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import static com.example.delayedretry.config.Queues.ORDER_STATUS_UPDATE_QUEUE;

@Configuration
public class AmqpConfig {

    private final RabbitConfigurationProperties rabbitProperties;
    private final QueueTopologyTemplate queueTopologyTemplate;

    public AmqpConfig(QueueTopologyTemplate queueTopologyTemplate, RabbitConfigurationProperties rabbitProperties) {
        this.queueTopologyTemplate = queueTopologyTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    @Bean
    public InitializingBean setupQueues() {
        return () -> queueTopologyTemplate.createQueue(ORDER_STATUS_UPDATE_QUEUE);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setMaxConcurrentConsumers(rabbitProperties.getMaxConcurrentConsumers());
        factory.setConcurrentConsumers(rabbitProperties.getConcurrentConsumers());
        factory.setPrefetchCount(rabbitProperties.getPrefetchCount());
        factory.setConnectionFactory(connectionFactory);
        factory.setAdviceChain(retryInterceptor());
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(false);
        return factory;
    }

    private RetryOperationsInterceptor retryInterceptor() {
        return queueTopologyTemplate.createDelayedRetryInterceptor(ExponentialDelayHandler.builder()
                .maxRetryAttempts(rabbitProperties.getMaxRetryAttempts())
                .initialDelay(rabbitProperties.getInitialDelay())
                .multiplier(rabbitProperties.getMultiplier())
                .maxDelay(rabbitProperties.getMaxDelay())
                .build());
    }
}
