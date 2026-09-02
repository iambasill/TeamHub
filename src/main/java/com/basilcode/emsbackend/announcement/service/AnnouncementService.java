package com.basilcode.emsbackend.announcement.service;

import com.basilcode.emsbackend.announcement.entity.Announcement;
import com.basilcode.emsbackend.announcement.enums.AnnouncementAudience;
import com.basilcode.emsbackend.announcement.repository.AnnouncementRepository;
import com.basilcode.emsbackend.board.entity.BoardMember;
import com.basilcode.emsbackend.board.service.BoardMembershipService;
import com.basilcode.emsbackend.common.exception.BadRequestException;
import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.notification.enums.NotificationType;
import com.basilcode.emsbackend.notification.service.NotificationService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.enums.UserTypeEnum;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Company-wide broadcast posts, each aimed at one or more {@link AnnouncementAudience}s. Posting
 * one drops a SYSTEM {@code Notification} into every matching recipient's inbox — reuses the
 * existing per-user notification pipeline rather than inventing a second delivery mechanism, so
 * the topbar bell is the one place everyone already checks. The frontend additionally polls
 * {@code GET /announcements} to pop unseen ones as a dismissible overlay (tracked client-side,
 * not here — see the frontend's local "seen announcement ids" store).
 */
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BoardMembershipService boardMembershipService;

    @Transactional
    public Announcement create(
            String title, String body, boolean pinned, OffsetDateTime expiresAt,
            List<String> requestedAudiences, User createdBy) {
        Set<AnnouncementAudience> audiences = resolveAudiences(requestedAudiences);

        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setBody(body);
        announcement.setPinned(pinned);
        announcement.setExpiresAt(expiresAt);
        announcement.setTargetAudiences(audiences);
        announcement.setCreatedBy(createdBy);
        Announcement saved = announcementRepository.save(announcement);

        String preview = body.length() > 140 ? body.substring(0, 140) + "…" : body;
        for (User recipient : recipientsFor(audiences)) {
            notificationService.create(recipient, title, preview, NotificationType.SYSTEM);
        }
        return saved;
    }

    /** Null/empty means "everyone," expressed internally as {@code {ALL}} — never left empty, so
     * {@link #matchesAudience} never has to special-case an empty set as a second meaning of "all." */
    private Set<AnnouncementAudience> resolveAudiences(List<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return Set.of(AnnouncementAudience.ALL);
        }
        Set<AnnouncementAudience> audiences = new LinkedHashSet<>();
        for (String raw : requested) {
            try {
                audiences.add(AnnouncementAudience.valueOf(raw.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid target audience: " + raw);
            }
        }
        return audiences;
    }

    private Set<User> recipientsFor(Set<AnnouncementAudience> audiences) {
        if (audiences.contains(AnnouncementAudience.ALL)) {
            return new LinkedHashSet<>(userRepository.findAll());
        }
        Set<User> recipients = new LinkedHashSet<>();
        for (AnnouncementAudience audience : audiences) {
            switch (audience) {
                case EMPLOYEE -> recipients.addAll(userRepository.findByUserTypeIn(List.of(UserTypeEnum.EMPLOYEE)));
                case HR -> recipients.addAll(userRepository.findByUserTypeIn(List.of(UserTypeEnum.HR)));
                case MANAGER -> recipients.addAll(userRepository.findByUserTypeIn(List.of(UserTypeEnum.MANAGER)));
                case ADMIN -> recipients.addAll(userRepository.findByUserTypeIn(List.of(UserTypeEnum.ADMIN)));
                case SUPER_ADMIN -> recipients.addAll(userRepository.findByUserTypeIn(List.of(UserTypeEnum.SUPER_ADMIN)));
                case BOARD_MEMBERS -> boardMembershipService.listMembers().stream()
                        .map(BoardMember::getUser).forEach(recipients::add);
                case ALL -> { /* handled above */ }
            }
        }
        return recipients;
    }

    /** Everything currently active, unfiltered — used by the admin compose/manage view where
     * seeing every announcement regardless of who it targets is the point. */
    public List<Announcement> listActive() {
        return announcementRepository.findActive(OffsetDateTime.now());
    }

    /** Everything currently active that this specific user is actually a target of — used by the
     * general employee-facing list and the "pop on screen" poll, so someone doesn't see (or get
     * nagged by) an announcement aimed at a roster they're not on. */
    public List<Announcement> listActiveFor(User user) {
        boolean isBoardMember = boardMembershipService.isMember(user.getId());
        return announcementRepository.findActive(OffsetDateTime.now()).stream()
                .filter(a -> matchesAudience(a, user, isBoardMember))
                .toList();
    }

    private boolean matchesAudience(Announcement announcement, User user, boolean isBoardMember) {
        Set<AnnouncementAudience> audiences = announcement.getTargetAudiences();
        if (audiences.isEmpty() || audiences.contains(AnnouncementAudience.ALL)) {
            return true;
        }
        for (AnnouncementAudience audience : audiences) {
            boolean matches = switch (audience) {
                case ALL -> true;
                case EMPLOYEE -> user.getUserType() == UserTypeEnum.EMPLOYEE;
                case HR -> user.getUserType() == UserTypeEnum.HR;
                case MANAGER -> user.getUserType() == UserTypeEnum.MANAGER;
                case ADMIN -> user.getUserType() == UserTypeEnum.ADMIN;
                case SUPER_ADMIN -> user.getUserType() == UserTypeEnum.SUPER_ADMIN;
                case BOARD_MEMBERS -> isBoardMember;
            };
            if (matches) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void delete(UUID id) {
        if (!announcementRepository.existsById(id)) {
            throw new NotFoundException("Announcement not found: " + id);
        }
        announcementRepository.deleteById(id);
    }
}
