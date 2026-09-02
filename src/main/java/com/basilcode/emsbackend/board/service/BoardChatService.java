package com.basilcode.emsbackend.board.service;

import com.basilcode.emsbackend.board.entity.BoardChatMessage;
import com.basilcode.emsbackend.board.entity.BoardDocument;
import com.basilcode.emsbackend.board.entity.BoardMember;
import com.basilcode.emsbackend.board.repository.BoardChatMessageRepository;
import com.basilcode.emsbackend.board.repository.BoardDocumentRepository;
import com.basilcode.emsbackend.board.repository.BoardMemberRepository;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.exception.UnAuthorizeException;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardChatService {

    private static final int HISTORY_LIMIT = 200;

    private final BoardChatMessageRepository boardChatMessageRepository;
    private final BoardDocumentRepository boardDocumentRepository;
    private final BoardMemberRepository boardMemberRepository;

    @Transactional
    public BoardChatMessage persist(User sender, String content, UUID attachmentDocumentId) {
        BoardChatMessage message = new BoardChatMessage();
        message.setSender(sender);
        message.setContent(content);
        if (attachmentDocumentId != null) {
            BoardDocument document = boardDocumentRepository.findById(attachmentDocumentId)
                    .orElseThrow(() -> new NotFoundException("Attachment document not found: " + attachmentDocumentId));
            message.setAttachmentDocument(document);
        }
        return boardChatMessageRepository.save(message);
    }

    /**
     * Most recent {@code HISTORY_LIMIT} messages, scoped to the requesting member's own tenure —
     * someone added to the board later shouldn't retroactively see discussion from before they
     * joined. Returned in chronological (oldest-first) order for display.
     */
    public List<BoardChatMessage> history(User requester) {
        BoardMember member = boardMemberRepository.findByUser_Id(requester.getId())
                .orElseThrow(() -> new NotFoundException("Not a board member: " + requester.getId()));

        List<BoardChatMessage> latestFirst = boardChatMessageRepository
                .findBySentAtGreaterThanEqualOrderBySentAtDesc(member.getAddedAt(), PageRequest.of(0, HISTORY_LIMIT));
        Collections.reverse(latestFirst);
        return latestFirst;
    }

    @Transactional
    public BoardChatMessage editMessage(UUID messageId, String newContent, User user) {
        BoardChatMessage message = boardChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));

        if (!message.getSender().getId().equals(user.getId())) {
            throw new UnAuthorizeException("You can only edit your own messages.");
        }

        long minutesSinceSent = java.time.Duration.between(message.getSentAt(), java.time.OffsetDateTime.now()).toMinutes();
        if (minutesSinceSent > 30) {
            throw new UnAuthorizeException("You can only edit messages within 30 minutes of sending.");
        }

        message.setContent(newContent);
        message.setEdited(true);
        return boardChatMessageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(UUID messageId, User user) {
        BoardChatMessage message = boardChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));

        boolean isAdmin = user.getUserType() == UserTypeEnum.ADMIN ||
                          user.getUserType() == UserTypeEnum.SUPER_ADMIN;
        boolean isSender = message.getSender().getId().equals(user.getId());

        if (!isAdmin && !isSender) {
            throw new UnAuthorizeException("You are not authorized to delete this message.");
        }

        if (isSender && !isAdmin) {
            long minutesSinceSent = java.time.Duration.between(message.getSentAt(), java.time.OffsetDateTime.now()).toMinutes();
            if (minutesSinceSent > 30) {
                throw new UnAuthorizeException("You can only delete messages within 30 minutes of sending.");
            }
        }

        boardChatMessageRepository.delete(message);
    }
}
