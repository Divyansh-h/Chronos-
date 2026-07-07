package com.taskflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${taskflow.rabbitmq.host:localhost}")
    private String relayHost;

    @Value("${taskflow.rabbitmq.port:61613}")
    private int relayPort;

    @Value("${taskflow.rabbitmq.username:guest}")
    private String clientLogin;

    @Value("${taskflow.rabbitmq.password:guest}")
    private String clientPasscode;

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableStompBrokerRelay("/topic")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(clientLogin)
                .setClientPasscode(clientPasscode)
                .setSystemLogin(clientLogin)
                .setSystemPasscode(clientPasscode);
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-endpoint")
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();
    }
}
