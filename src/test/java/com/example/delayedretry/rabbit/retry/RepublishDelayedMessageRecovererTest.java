package com.example.delayedretry.rabbit.retry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepublishDelayedMessageRecovererTest {

    ExponentialDelayHandler delayHandlerMock;
    MessageRecoverer retryMessageRecovererMock;
    MessageRecoverer errorMessageRecovererMock;

    RepublishDelayedMessageRecoverer target;

    @BeforeEach
    void setUp() {
        delayHandlerMock = mock(ExponentialDelayHandler.class);
        retryMessageRecovererMock = mock(MessageRecoverer.class);
        errorMessageRecovererMock = mock(MessageRecoverer.class);
        target = new RepublishDelayedMessageRecoverer(retryMessageRecovererMock, errorMessageRecovererMock, delayHandlerMock);
    }


    @Test
    void shouldRepublishMessageWithExpirationPropertyToRetryMessageRecoverer() {
        // given
        Throwable cause = new RuntimeException("Expected test exception");


        // and a message with a "x-death" header with a "count"
        long retryCount = 1L;
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeaders(Map.of("x-death", List.of(Map.of("count", retryCount))));

        Message message = mock(Message.class);
        when(message.getMessageProperties()).thenReturn(messageProperties);

        // and a delay handler that can retry
        when(delayHandlerMock.canRetry(retryCount)).thenReturn(true);

        // and a delay handler that returns a delay
        String delay = "1000";
        when(delayHandlerMock.getDelay((int) retryCount)).thenReturn(delay);

        // when
        target.recover(message, cause);

        // then
        verify(retryMessageRecovererMock, times(1))
                .recover(argThat(message1 -> message1.getMessageProperties().getExpiration().equals(delay)), any());

        verify(errorMessageRecovererMock, never()).recover(message, cause);
    }

    @Test
    void shouldRepublishMessageWithExpirationPropertyToRetryMessageRecovererWhenXDeadHeadersIsNull() {
        // given
        Throwable cause = new RuntimeException("Expected test exception");


        // and a message with a "x-death" header with a "count"
        Message message = mock(Message.class);
        MessageProperties messageProperties = new MessageProperties();
        when(message.getMessageProperties()).thenReturn(messageProperties);

        // and a delay handler that can retry
        when(delayHandlerMock.canRetry(anyLong())).thenReturn(true);

        // and a delay handler that returns a delay
        String delay = "1000";
        when(delayHandlerMock.getDelay(anyInt())).thenReturn(delay);

        // when
        target.recover(message, cause);

        // then
        verify(retryMessageRecovererMock, times(1))
                .recover(argThat(message1 -> message1.getMessageProperties().getExpiration().equals(delay)), any());

        verify(errorMessageRecovererMock, never()).recover(message, cause);
    }

    @Test
    void shouldRepublishMessageToErrorMessageRecovererWhenDelayHandlerCantRetry() {
        // given
        Throwable cause = new RuntimeException("Expected test exception");

        // and a message with a "x-death" header with a "count"
        long retryCount = 1L;
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeaders(Map.of("x-death", List.of(Map.of("count", retryCount))));

        Message message = mock(Message.class);
        when(message.getMessageProperties()).thenReturn(messageProperties);

        // and a delay handler that can retry
        when(delayHandlerMock.canRetry(retryCount)).thenReturn(false);

        // when
        target.recover(message, cause);

        // then

        verify(delayHandlerMock, never()).getDelay((int) retryCount);

        verify(retryMessageRecovererMock, never()).recover(any(), any());

        verify(errorMessageRecovererMock, times(1)).recover(message, cause);
    }
}