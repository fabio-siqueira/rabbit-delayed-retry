package com.example.delayedretry;

import com.example.delayedretry.config.Queues;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class Listener {

    private final Logger log = Logger.getLogger(Listener.class.getName());

    @RabbitListener(queues = Queues.ORDER_STATUS_UPDATE_QUEUE)
    public void listen(String in) {
        log.info("\n");
        log.info(String.format("Message received. message=%s",in));
        throw new RuntimeException("Expected exception");
    }
}
