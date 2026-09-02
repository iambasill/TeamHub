package com.basilcode.emsbackend.board.repository;

import com.basilcode.emsbackend.board.entity.BoardActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardActivityLogRepository extends JpaRepository<BoardActivityLog, UUID> {
    List<BoardActivityLog> findByUser_IdOrderByOccurredAtDesc(UUID userId);
    List<BoardActivityLog> findAllByOrderByOccurredAtDesc();
    boolean existsByUser_IdAndOccurredAtAfter(UUID userId, OffsetDateTime after);
    Optional<BoardActivityLog> findTopByUser_IdOrderByOccurredAtDesc(UUID userId);
}
