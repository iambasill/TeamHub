package com.basilcode.emsbackend.board.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BoardChatMessageDto(
        UUID id,
        UUID senderId,
        String senderName,
        String senderProfilePictureUrl,
        String content,
        UUID attachmentDocumentId,
        String attachmentFileName,
        OffsetDateTime sentAt,
        boolean isEdited
) {
}
