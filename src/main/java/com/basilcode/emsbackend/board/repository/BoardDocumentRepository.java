package com.basilcode.emsbackend.board.repository;

import com.basilcode.emsbackend.board.entity.BoardDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardDocumentRepository extends JpaRepository<BoardDocument, UUID> {
    /**
     * Loads full entities including {@code content} — acceptable at this app's scale, but a
     * projection excluding the byte[] column would be worth adding if document volume grows
     * large enough for listing to become a real cost.
     */
    List<BoardDocument> findAllByOrderByUploadedAtDesc();
}
