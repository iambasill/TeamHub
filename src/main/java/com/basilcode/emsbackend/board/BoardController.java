package com.basilcode.emsbackend.board;

import com.basilcode.emsbackend.board.dto.AddBoardMemberRequest;
import com.basilcode.emsbackend.board.dto.BoardActivityLogDto;
import com.basilcode.emsbackend.board.dto.BoardMemberDto;
import com.basilcode.emsbackend.board.entity.BoardActivityLog;
import com.basilcode.emsbackend.board.entity.BoardMember;
import com.basilcode.emsbackend.board.service.BoardMembershipService;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Board Room membership management and audit trail. Roster changes require {@code ROLE_ADMIN};
 * everything else requires actual board membership, checked via {@link BoardMembershipService}
 * rather than role — the whole point of this feature is that the two are independent.
 */
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardMembershipService boardMembershipService;
    private final UserRepository userRepository;

    /** No role/membership restriction beyond being authenticated — every user needs to be able
     * to check their own status to know whether to render the Board Room section at all. */
    @GetMapping("/membership/me")
    public ResponseEntity<ApiResponse<Boolean>> isCurrentUserMember(@AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
        return ResponseEntity.ok(ApiResponse.success("Membership checked", boardMembershipService.isMember(user.getId())));
    }

    @PostMapping("/enter")
    @PreAuthorize("@boardMembershipService.isCurrentUserMember(authentication)")
    public ResponseEntity<ApiResponse<Void>> enter(@AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
        boardMembershipService.recordEntry(user);
        return ResponseEntity.ok(ApiResponse.success("Entry recorded"));
    }

    @GetMapping("/members")
    @PreAuthorize("@boardMembershipService.isCurrentUserMember(authentication)")
    public ResponseEntity<ApiResponse<List<BoardMemberDto>>> listMembers() {
        List<BoardMemberDto> members = boardMembershipService.listMembers().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Board members retrieved", members));
    }

    @PostMapping("/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BoardMemberDto>> addMember(
            @Valid @RequestBody AddBoardMemberRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        User addedBy = userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
        BoardMember member = boardMembershipService.addMember(request.userId(), addedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Board member added", toDto(member)));
    }

    @DeleteMapping("/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable UUID userId) {
        boardMembershipService.removeMember(userId);
        return ResponseEntity.ok(ApiResponse.success("Board member removed"));
    }

    @GetMapping("/activity-log")
    @PreAuthorize("@boardMembershipService.isCurrentUserMember(authentication)")
    public ResponseEntity<ApiResponse<List<BoardActivityLogDto>>> getActivityLog(
            @RequestParam(required = false) UUID userId) {
        List<BoardActivityLogDto> log = boardMembershipService.activityLog(userId).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Board activity log retrieved", log));
    }

    private BoardMemberDto toDto(BoardMember member) {
        User user = member.getUser();
        User addedBy = member.getAddedBy();
        return new BoardMemberDto(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmailId(),
                user.getUserType() != null ? user.getUserType().name() : "EMPLOYEE",
                addedBy != null ? addedBy.getFirstName() + " " + addedBy.getLastName() : "System",
                member.getAddedAt(),
                boardMembershipService.isActive(user.getId()),
                boardMembershipService.lastActivityAt(user.getId()).orElse(null));
    }

    private BoardActivityLogDto toDto(BoardActivityLog log) {
        User user = log.getUser();
        return new BoardActivityLogDto(
                log.getId(), user.getId(), user.getFirstName() + " " + user.getLastName(),
                log.getActivityType().name(), log.getDescription(), log.getOccurredAt());
    }
}
