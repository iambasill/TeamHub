package com.basilcode.emsbackend.board.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class BoardWebSocketConfig implements WebSocketConfigurer {

    private final BoardWebSocketHandler boardWebSocketHandler;
    private final BoardWebSocketAuthInterceptor boardWebSocketAuthInterceptor;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(boardWebSocketHandler, "/ws/board")
                .addInterceptors(boardWebSocketAuthInterceptor)
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
    }
}
