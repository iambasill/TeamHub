package com.basilcode.emsbackend.board.ws;

import com.basilcode.emsbackend.auth.jwt.JwtService;
import com.basilcode.emsbackend.board.service.BoardMembershipService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * The WebSocket-layer equivalent of the {@code @PreAuthorize} checks used everywhere else in
 * this codebase: rejects the handshake (closing with 401/403 before any {@link
 * BoardWebSocketHandler} method runs) unless the same {@code accessToken} cookie {@code
 * JwtAuthenticationFilter} reads for REST requests is present, valid, and belongs to a board
 * member.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BoardWebSocketAuthInterceptor implements HandshakeInterceptor {

    public static final String USER_ATTRIBUTE = "boardUser";

    private final JwtService jwtService;
    private final UserService userService;
    private final BoardMembershipService boardMembershipService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        String jwt = extractJwt(servletRequest.getServletRequest());
        if (jwt == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String email = jwtService.extractUsername(jwt);
            UserDetails userDetails = userService.loadUserByUsername(email);
            if (!jwtService.isTokenValid(jwt, userDetails)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            User user = (User) userDetails;
            if (!boardMembershipService.isMember(user.getId())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }

            attributes.put(USER_ATTRIBUTE, user);
            return true;
        } catch (Exception e) {
            log.warn("Board WebSocket handshake rejected: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractJwt(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
