package com.basilcode.emsbackend.board.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class BoardWebSocketConfig implements WebSocketConfigurer {

    private final BoardWebSocketHandler boardWebSocketHandler;
    private final BoardWebSocketAuthInterceptor boardWebSocketAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(boardWebSocketHandler, "/ws/board")
                .addInterceptors(boardWebSocketAuthInterceptor)
                .setAllowedOrigins("http://localhost:3000", "http://localhost:8000");
    }
}
