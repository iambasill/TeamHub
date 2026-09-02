package com.basilcode.emsbackend.board.repository;

import com.basilcode.emsbackend.board.entity.BoardChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BoardChatMessageRepository extends JpaRepository<BoardChatMessage, UUID> {
    /** Most recent first — the service reverses this to chronological order for display. */
    List<BoardChatMessage> findAllByOrderBySentAtDesc(Pageable pageable);

    /** Scoped to a member's own tenure — someone added later shouldn't see history that predates their membership. */
    List<BoardChatMessage> findBySentAtGreaterThanEqualOrderBySentAtDesc(OffsetDateTime sentAt, Pageable pageable);
}
