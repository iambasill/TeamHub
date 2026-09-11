package com.basilcode.emsbackend.board;

import com.basilcode.emsbackend.board.dto.BoardChatMessageDto;
import com.basilcode.emsbackend.board.entity.BoardChatMessage;
import com.basilcode.emsbackend.board.entity.BoardDocument;
import com.basilcode.emsbackend.board.service.BoardChatService;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only history for the Board Room chat — sending happens over the {@code /ws/board}
 * WebSocket (see {@link com.basilcode.emsbackend.board.ws.BoardWebSocketHandler}), which persists
 * via {@link BoardChatService} and rebroadcasts, so a REST POST here would be redundant.
 */
@RestController
@RequestMapping("/board/chat")
@RequiredArgsConstructor
@PreAuthorize("@boardMembershipService.isCurrentUserMember(authentication)")
public class BoardChatController {

    private final BoardChatService boardChatService;
    private final UserRepository userRepository;

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<BoardChatMessageDto>>> getHistory(@AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
        List<BoardChatMessageDto> messages = boardChatService.history(user).stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success("Chat history retrieved", messages));
    }

    private BoardChatMessageDto toDto(BoardChatMessage message) {
        BoardDocument attachment = message.getAttachmentDocument();
        return new BoardChatMessageDto(
                message.getId(),
                message.getSender().getId(),
                message.getSender().getFirstName() + " " + message.getSender().getLastName(),
                message.getSender().getProfilePictureUrl(),
                message.getContent(),
                attachment != null ? attachment.getId() : null,
                attachment != null ? attachment.getFileName() : null,
                message.getSentAt(),
                message.isEdited());
    }
}
