package com.taskflow.service;

import com.taskflow.dto.WorkflowEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public void publishEvent(WorkflowEvent event) {
        log.debug("Publishing WS event: {}", event);
        simpMessagingTemplate.convertAndSend("/topic/workflow-events", event);
    }
}
