package com.taskflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.WorkflowEvent;
import com.taskflow.service.EventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WebSocketBrokerIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EventPublisherService eventPublisherService;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @Test
    void shouldReceivePublishedEventOverStomp() throws ExecutionException, InterruptedException, TimeoutException {
        // Arrange: Connect directly to the underlying WebSocket path used by SockJS
        String wsUrl = "ws://localhost:" + port + "/ws-endpoint/websocket"; 
        CompletableFuture<WorkflowEvent> completableFuture = new CompletableFuture<>();

        StompSession session = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                // Subscribe to the public topic our React frontend uses
                session.subscribe("/topic/workflow-events", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return WorkflowEvent.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        completableFuture.complete((WorkflowEvent) payload);
                    }
                });
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                completableFuture.completeExceptionally(exception);
            }
        }).get(5, TimeUnit.SECONDS);

        // Wait a brief moment to ensure the STOMP subscription propagates on the embedded Tomcat server
        Thread.sleep(1000);

        UUID workflowId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        WorkflowEvent testEvent = new WorkflowEvent("TASK_UPDATE", workflowId, taskId, "RUNNING");

        // Act: Have the backend service broadcast an event just like it does when a worker claims a task
        eventPublisherService.publishEvent(testEvent);

        // Assert: The independent WebSocket client should receive the identical JSON payload
        WorkflowEvent receivedEvent = completableFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(receivedEvent);
        assertEquals("TASK_UPDATE", receivedEvent.eventType());
        assertEquals(workflowId, receivedEvent.workflowId());
        assertEquals(taskId, receivedEvent.taskId());
        assertEquals("RUNNING", receivedEvent.status());

        session.disconnect();
    }
}
