package com.basilcode.emsbackend.board.ws;

import com.basilcode.emsbackend.board.entity.BoardChatMessage;
import com.basilcode.emsbackend.board.event.BoardMembershipRevokedEvent;
import com.basilcode.emsbackend.board.service.BoardChatService;
import com.basilcode.emsbackend.board.service.BoardMembershipService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * One logical "room" for the whole Board — appropriate at this app's scale (a handful of
 * directors), so there's no per-session-id room routing. A small JSON envelope carries every
 * message type: {@code chat} (persisted then rebroadcast to everyone), {@code signal} (a dumb
 * relay for WebRTC SDP/ICE exchange — this class never inspects signaling contents), and {@code
 * presence} (mic-mute state, rebroadcast to everyone). Connection/disconnection also broadcast
 * presence automatically.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BoardWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final BoardChatService boardChatService;
    private final BoardMembershipService boardMembershipService;

    /** Send timeout / buffer cap for the decorator below — a slow/stuck client gets dropped
     * rather than blocking the broadcaster thread indefinitely. */
    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, User> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionMuted = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionInCall = new ConcurrentHashMap<>();
    private final Map<String, Boolean> sessionHasVideo = new ConcurrentHashMap<>();
    /** Set when the first participant joins a call, cleared when the last one leaves — lets a
     * client joining an already-live call learn the true elapsed duration, not just its own. */
    private final AtomicReference<Instant> callStartedAt = new AtomicReference<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = (User) session.getAttributes().get(BoardWebSocketAuthInterceptor.USER_ATTRIBUTE);
        // Raw WebSocketSession.sendMessage() isn't thread-safe — two broadcasts firing from two
        // different request threads at nearly the same instant (e.g. one user's "presence" update
        // overlapping another's "typing" event) can both try to write to the same target session
        // concurrently, which the underlying container rejects with "TEXT_PARTIAL_WRITING is an
        // invalid state". This decorator serializes sends per session so that can't happen.
        WebSocketSession concurrentSafeSession =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_SIZE_LIMIT_BYTES);
        sessions.put(session.getId(), concurrentSafeSession);
        sessionUsers.put(session.getId(), user);
        sessionMuted.put(session.getId(), Boolean.FALSE);
        sessionInCall.put(session.getId(), Boolean.FALSE);
        sessionHasVideo.put(session.getId(), Boolean.FALSE);
        boardMembershipService.recordEntry(user);
        broadcastPresence();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> envelope = objectMapper.readValue(message.getPayload(), new TypeReference<Map<String, Object>>() {
        });
        User sender = sessionUsers.get(session.getId());
        if (sender == null) {
            return;
        }
        // Backstop: handshake-time membership checks don't catch a mid-session revocation, and
        // the event-driven disconnect below can in principle race a message already in flight —
        // this closes that gap so a removed member can never take another action even briefly.
        if (!boardMembershipService.isMember(sender.getId())) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Board membership revoked"));
            return;
        }

        String type = String.valueOf(envelope.get("type"));
        switch (type) {
            case "chat" -> handleChat(sender, envelope);
            case "signal" -> handleSignal(sender, envelope);
            case "presence" -> {
                sessionMuted.put(session.getId(), Boolean.TRUE.equals(envelope.get("muted")));
                sessionInCall.put(session.getId(), Boolean.TRUE.equals(envelope.get("inCall")));
                sessionHasVideo.put(session.getId(), Boolean.TRUE.equals(envelope.get("hasVideo")));
                broadcastPresence();
            }
            case "ring" -> handleRing(sender);
            case "force-mute" -> handleForceMute(sender, envelope);
            case "end-call" -> handleEndCall(sender);
            case "typing" -> handleTyping(sender, envelope);
            case "edit-chat" -> handleEditChat(sender, envelope);
            case "delete-chat" -> handleDeleteChat(sender, envelope);
            default -> log.warn("Unknown board WebSocket message type: {}", type);
        }
    }

    /**
     * Fires the instant an admin removes someone from the board — closes their live socket right
     * away instead of leaving them able to chat/call until they happen to disconnect on their own.
     */
    @EventListener
    public void onMembershipRevoked(BoardMembershipRevokedEvent event) {
        String revokedUserId = event.getUserId().toString();
        sessionUsers.entrySet().stream()
                .filter(entry -> entry.getValue().getId().toString().equals(revokedUserId))
                .map(Map.Entry::getKey)
                .map(sessions::get)
                .filter(session -> session != null && session.isOpen())
                .forEach(session -> {
                    try {
                        session.close(CloseStatus.POLICY_VIOLATION.withReason("Board membership revoked"));
                    } catch (Exception e) {
                        log.warn("Failed to close session for revoked board member {}: {}", revokedUserId, e.getMessage());
                    }
                });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        sessionUsers.remove(session.getId());
        sessionMuted.remove(session.getId());
        sessionInCall.remove(session.getId());
        sessionHasVideo.remove(session.getId());
        broadcastPresence();
    }

    /** Purely ephemeral — never persisted, just relayed to everyone else live. */
    private void handleTyping(User sender, Map<String, Object> envelope) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "typing");
        out.put("userId", sender.getId().toString());
        out.put("name", sender.getFirstName() + " " + sender.getLastName());
        out.put("isTyping", Boolean.TRUE.equals(envelope.get("isTyping")));
        broadcast(out);
    }

    private void handleChat(User sender, Map<String, Object> envelope) throws Exception {
        Object contentObj = envelope.get("content");
        if (!(contentObj instanceof String content) || content.isBlank()) {
            return;
        }
        Object attachmentIdObj = envelope.get("attachmentDocumentId");
        UUID attachmentId = attachmentIdObj instanceof String s ? UUID.fromString(s) : null;

        BoardChatMessage saved = boardChatService.persist(sender, content, attachmentId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "chat");
        out.put("id", saved.getId().toString());
        out.put("senderId", sender.getId().toString());
        out.put("senderName", sender.getFirstName() + " " + sender.getLastName());
        out.put("senderProfilePictureUrl", sender.getProfilePictureUrl());
        out.put("content", saved.getContent());
        out.put("attachmentDocumentId", attachmentId != null ? attachmentId.toString() : null);
        out.put("sentAt", saved.getSentAt().toString());
        out.put("isEdited", saved.isEdited());
        broadcast(out);
    }

    private void handleEditChat(User sender, Map<String, Object> envelope) throws Exception {
        Object messageIdObj = envelope.get("messageId");
        Object contentObj = envelope.get("content");
        if (!(messageIdObj instanceof String messageId) || !(contentObj instanceof String content) || content.isBlank()) {
            return;
        }

        BoardChatMessage saved = boardChatService.editMessage(UUID.fromString(messageId), content, sender);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "edit-chat");
        out.put("messageId", saved.getId().toString());
        out.put("content", saved.getContent());
        out.put("isEdited", saved.isEdited());
        broadcast(out);
    }

    private void handleDeleteChat(User sender, Map<String, Object> envelope) throws Exception {
        Object messageIdObj = envelope.get("messageId");
        if (!(messageIdObj instanceof String messageId)) {
            return;
        }

        boardChatService.deleteMessage(UUID.fromString(messageId), sender);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "delete-chat");
        out.put("messageId", messageId);
        broadcast(out);
    }

    /** A dumb relay — this handler never reads or reasons about the WebRTC SDP/ICE payload. */
    private void handleSignal(User sender, Map<String, Object> envelope) throws Exception {
        Object targetIdObj = envelope.get("targetUserId");
        if (!(targetIdObj instanceof String targetUserId)) {
            return;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "signal");
        out.put("fromUserId", sender.getId().toString());
        out.put("payload", envelope.get("payload"));
        sendToUser(targetUserId, out);
    }

    /** Only ADMIN/SUPER_ADMIN can ring the room — enforced here too, not just by hiding the button client-side. */
    private void handleRing(User sender) throws Exception {
        if (sender.getUserType() != UserTypeEnum.ADMIN && sender.getUserType() != UserTypeEnum.SUPER_ADMIN) {
            log.warn("Non-admin user {} attempted to ring the board room", sender.getId());
            return;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "ring");
        out.put("fromUserId", sender.getId().toString());
        out.put("fromName", sender.getFirstName() + " " + sender.getLastName());
        broadcast(out);
    }

    /**
     * An ADMIN/SUPER_ADMIN can end the call for the whole room, not just leave it themselves —
     * everyone else's client receives this and hangs up locally. A non-admin's own "leave" never
     * reaches this handler at all; their client just tears down its own call state.
     */
    private void handleEndCall(User sender) throws Exception {
        if (sender.getUserType() != UserTypeEnum.ADMIN && sender.getUserType() != UserTypeEnum.SUPER_ADMIN) {
            log.warn("Non-admin user {} attempted to end the call for everyone", sender.getId());
            return;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "end-call");
        out.put("fromUserId", sender.getId().toString());
        broadcast(out);
    }

    /**
     * A call moderator (any ADMIN/SUPER_ADMIN currently in the call) can force-mute another
     * participant. The target's own client executes the mute on its own track — no one else can
     * reach into another browser's microphone, so this is a relayed instruction the target
     * client is trusted to honor, exactly like the WebRTC signal relay above.
     */
    private void handleForceMute(User sender, Map<String, Object> envelope) throws Exception {
        if (sender.getUserType() != UserTypeEnum.ADMIN && sender.getUserType() != UserTypeEnum.SUPER_ADMIN) {
            log.warn("Non-admin user {} attempted to force-mute a participant", sender.getId());
            return;
        }
        Object targetIdObj = envelope.get("targetUserId");
        if (!(targetIdObj instanceof String targetUserId)) {
            return;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "force-mute");
        out.put("fromUserId", sender.getId().toString());
        sendToUser(targetUserId, out);
    }

    private void sendToUser(String targetUserId, Map<String, Object> payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        for (Map.Entry<String, User> entry : sessionUsers.entrySet()) {
            if (entry.getValue().getId().toString().equals(targetUserId)) {
                String sessionId = entry.getKey();
                WebSocketSession targetSession = sessions.get(sessionId);
                if (targetSession != null && targetSession.isOpen()) {
                    try {
                        targetSession.sendMessage(new TextMessage(json));
                    } catch (Exception e) {
                        log.warn("Failed to send board WebSocket message to session {}: {}", sessionId, e.getMessage());
                        forgetDeadSession(sessionId);
                    }
                }
            }
        }
    }

    /**
     * Deduplicated by user ID, not session ID — a single user can briefly (or, in a dev
     * environment with hot-reload/StrictMode remounts, not-so-briefly) hold more than one open
     * socket, and should still only ever appear once in the roster. When a user has multiple
     * sessions, "in call" wins if any of them is (OR), and the most-recently-updated session's
     * mute state is used.
     */
    private void broadcastPresence() throws Exception {
        Map<String, Map<String, Object>> byUserId = new LinkedHashMap<>();
        for (Map.Entry<String, User> entry : sessionUsers.entrySet()) {
            String sessionId = entry.getKey();
            User user = entry.getValue();
            String userId = user.getId().toString();
            boolean inCall = Boolean.TRUE.equals(sessionInCall.get(sessionId));
            boolean hasVideo = Boolean.TRUE.equals(sessionHasVideo.get(sessionId));

            Map<String, Object> existing = byUserId.get(userId);
            if (existing != null) {
                existing.put("muted", Boolean.TRUE.equals(sessionMuted.get(sessionId)));
                existing.put("inCall", Boolean.TRUE.equals(existing.get("inCall")) || inCall);
                existing.put("hasVideo", Boolean.TRUE.equals(existing.get("hasVideo")) || hasVideo);
                continue;
            }

            Map<String, Object> p = new LinkedHashMap<>();
            p.put("userId", userId);
            p.put("name", user.getFirstName() + " " + user.getLastName());
            p.put("profilePictureUrl", user.getProfilePictureUrl());
            p.put("muted", Boolean.TRUE.equals(sessionMuted.get(sessionId)));
            p.put("inCall", inCall);
            p.put("hasVideo", hasVideo);
            byUserId.put(userId, p);
        }

        boolean anyoneInCall = byUserId.values().stream().anyMatch(p -> Boolean.TRUE.equals(p.get("inCall")));
        if (anyoneInCall) {
            callStartedAt.compareAndSet(null, Instant.now());
        } else {
            callStartedAt.set(null);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "presence");
        out.put("participants", List.copyOf(byUserId.values()));
        Instant startedAt = callStartedAt.get();
        out.put("callStartedAt", startedAt != null ? startedAt.toString() : null);
        broadcast(out);
    }

    private void broadcast(Map<String, Object> payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (Exception e) {
                    log.warn("Failed to send board WebSocket message to session {}: {}", session.getId(), e.getMessage());
                    forgetDeadSession(session.getId());
                }
            }
        }
    }

    /**
     * A send failure (aborted TCP connection, etc.) means this session is effectively already
     * gone even if {@code afterConnectionClosed} hasn't fired yet — without this, every future
     * broadcast would keep retrying the same dead session forever, which is exactly why the same
     * session ID could show up in the logs repeatedly, minutes apart.
     */
    private void forgetDeadSession(String sessionId) {
        WebSocketSession dead = sessions.remove(sessionId);
        sessionUsers.remove(sessionId);
        sessionMuted.remove(sessionId);
        sessionInCall.remove(sessionId);
        sessionHasVideo.remove(sessionId);
        if (dead != null) {
            try {
                dead.close(CloseStatus.SERVER_ERROR);
            } catch (Exception ignored) {
                // already gone — nothing more to close
            }
        }
    }
}
