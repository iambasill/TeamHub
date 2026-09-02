package com.basilcode.emsbackend.notification;

import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.common.response.ApiResponse;
import com.basilcode.emsbackend.notification.dto.NotificationDto;
import com.basilcode.emsbackend.notification.entity.Notification;
import com.basilcode.emsbackend.notification.service.NotificationService;
import com.basilcode.emsbackend.user.entity.User;
import com.basilcode.emsbackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Every user only ever sees/mutates their own notifications — no admin override needed here. */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getMyNotifications(
            @AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        List<NotificationDto> notifications = notificationService.listForUser(user.getId()).stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", notificationService.unreadCount(user.getId())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID id, @AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        notificationService.markRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal UserDetails currentUser) {
        User user = resolveUser(currentUser);
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked read"));
    }

    private User resolveUser(UserDetails currentUser) {
        return userRepository.findByEmailId(currentUser.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found: " + currentUser.getUsername()));
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getTitle(), n.getDescription(), n.getType().name(),
                !n.isRead(), n.getCreatedAt());
    }
}
