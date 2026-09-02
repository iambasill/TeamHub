package com.basilcode.emsbackend.board.event;

import java.util.UUID;

/**
 * Published when a user is removed from the board roster — {@link
 * com.basilcode.emsbackend.board.ws.BoardWebSocketHandler} listens for this to forcibly
 * disconnect any live session that user still holds. Membership is checked at WebSocket
 * handshake time only, so without this, a removed member keeps their existing connection (and
 * can keep chatting) until they happen to disconnect and try to reconnect on their own.
 */
public class BoardMembershipRevokedEvent {
    private final UUID userId;

    public BoardMembershipRevokedEvent(UUID userId) {
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }
}
