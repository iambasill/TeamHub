package com.basilcode.emsbackend.board.service;

import com.basilcode.emsbackend.board.entity.BoardActivityLog;
import com.basilcode.emsbackend.board.entity.BoardMember;
import com.basilcode.emsbackend.board.enums.BoardActivityType;
import com.basilcode.emsbackend.board.event.BoardMembershipRevokedEvent;
import com.basilcode.emsbackend.board.repository.BoardActivityLogRepository;
import com.basilcode.emsbackend.board.repository.BoardMemberRepository;
import com.basilcode.emsbackend.common.exception.AlreadyExistsException;
import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.mailService.MailService;
import com.basilcode.emsbackend.notification.enums.NotificationType;
import com.basilcode.emsbackend.notification.service.NotificationService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Board Room access control — deliberately independent of {@link com.basilcode.emsbackend.user.enums.UserTypeEnum}.
 * Membership (this table) is what grants access; {@code hasRole('ADMIN')} only gates who may
 * change the roster, never who may enter it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoardMembershipService {

    private static final Duration ACTIVE_WINDOW = Duration.ofMinutes(5);
    private static final String SYSTEM_NAME = "the Board Room";

    private final BoardMemberRepository boardMemberRepository;
    private final BoardActivityLogRepository boardActivityLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final ApplicationEventPublisher eventPublisher;

    public boolean isMember(UUID userId) {
        return boardMemberRepository.existsByUser_Id(userId);
    }

    /** Referenced directly from {@code @PreAuthorize("@boardMembershipService.isCurrentUserMember(authentication)")}. */
    public boolean isCurrentUserMember(Authentication authentication) {
        return userRepository.findByEmailId(authentication.getName())
                .map(user -> isMember(user.getId()))
                .orElse(false);
    }

    @Transactional
    public BoardMember addMember(UUID targetUserId, User addedBy) {
        if (boardMemberRepository.existsByUser_Id(targetUserId)) {
            throw new AlreadyExistsException("User is already a board member");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetUserId));

        BoardMember member = new BoardMember();
        member.setUser(target);
        member.setAddedBy(addedBy);
        BoardMember saved = boardMemberRepository.save(member);

        String addedByName = addedBy.getFirstName() + " " + addedBy.getLastName();
        notificationService.create(target, "Board Room Access Granted",
                "You've been added to the Board Room roster by " + addedByName + ".",
                NotificationType.BOARD);
        sendAccessEmail(target, "granted", addedByName);

        return saved;
    }

    @Transactional
    public void removeMember(UUID targetUserId) {
        BoardMember member = boardMemberRepository.findByUser_Id(targetUserId)
                .orElseThrow(() -> new NotFoundException("User is not a board member: " + targetUserId));
        if (member.getUser().getUserType() == UserTypeEnum.SUPER_ADMIN) {
            throw new BadRequestException("Super admins cannot be removed from the board roster");
        }
        boardMemberRepository.delete(member);
        notificationService.create(member.getUser(), "Board Room Access Removed",
                "You've been removed from the Board Room roster.", NotificationType.BOARD);
        sendAccessEmail(member.getUser(), "removed", null);
        eventPublisher.publishEvent(new BoardMembershipRevokedEvent(targetUserId));
    }

    /**
     * Email is a courtesy on top of the in-app notification (the source of truth) — an SMTP
     * outage must never roll back a membership change, so failures are logged, not thrown.
     */
    private void sendAccessEmail(User target, String action, String addedByName) {
        try {
            String template = "granted".equals(action)
                    ? "com/basilcode/emsbackend/notification/template/access-granted-email.html"
                    : "com/basilcode/emsbackend/notification/template/access-removed-email.html";
            String body = mailService.renderTemplate(template, Map.of(
                    "firstName", target.getFirstName(),
                    "systemName", SYSTEM_NAME,
                    "addedByName", addedByName != null ? addedByName : ""
            ));
            String subject = "granted".equals(action) ? "You've been added to the Board Room" : "Board Room access removed";
            mailService.sendMail(target.getEmailId(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send board access email to {}: {}", target.getEmailId(), e.getMessage());
        }
    }

    public List<BoardMember> listMembers() {
        return boardMemberRepository.findAll();
    }

    public void recordEntry(User user) {
        log(user, BoardActivityType.ENTERED, null);
    }

    public void recordAction(User user, String description) {
        log(user, BoardActivityType.ACTION, description);
    }

    public boolean isActive(UUID userId) {
        return boardActivityLogRepository.existsByUser_IdAndOccurredAtAfter(
                userId, OffsetDateTime.now().minus(ACTIVE_WINDOW));
    }

    public Optional<OffsetDateTime> lastActivityAt(UUID userId) {
        return boardActivityLogRepository.findTopByUser_IdOrderByOccurredAtDesc(userId)
                .map(BoardActivityLog::getOccurredAt);
    }

    public List<BoardActivityLog> activityLog(UUID userId) {
        return userId != null
                ? boardActivityLogRepository.findByUser_IdOrderByOccurredAtDesc(userId)
                : boardActivityLogRepository.findAllByOrderByOccurredAtDesc();
    }

    private void log(User user, BoardActivityType type, String description) {
        BoardActivityLog entry = new BoardActivityLog();
        entry.setUser(user);
        entry.setActivityType(type);
        entry.setDescription(description);
        boardActivityLogRepository.save(entry);
    }
}
