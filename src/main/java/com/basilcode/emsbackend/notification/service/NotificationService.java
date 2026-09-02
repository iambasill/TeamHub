package com.basilcode.emsbackend.notification.service;

import com.basilcode.emsbackend.common.exception.NotFoundException;
import com.basilcode.emsbackend.notification.entity.Notification;
import com.basilcode.emsbackend.notification.enums.NotificationType;
import com.basilcode.emsbackend.notification.repository.NotificationRepository;
import com.basilcode.emsbackend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Real, per-user notifications. Unread counts are cache-aside cached in Redis — the DB
 * (`notifications.read`) is always the source of truth; Redis only exists so the topbar badge
 * doesn't run a COUNT query on every render. A cache miss (cold start, Redis restart, key
 * eviction) transparently recomputes from the DB and repopulates the cache, so correctness never
 * depends on Redis being up — only responsiveness does.
 *
 * <p>Every Redis call is wrapped so a connection failure never propagates: an uncaught
 * exception here would run inside the same {@code @Transactional} method as the DB write and
 * roll that back too, silently undoing a real "mark as read" because a cache happened to be
 * unreachable. Worst case without Redis: {@link #unreadCount} falls back to a live COUNT query
 * every time — slower, never wrong.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final Duration UNREAD_COUNT_TTL = Duration.ofHours(1);

    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public Notification create(User recipient, String title, String description, NotificationType type) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setType(type);
        Notification saved = notificationRepository.save(notification);
        bumpUnreadCache(recipient.getId(), 1);
        return saved;
    }

    public List<Notification> listForUser(UUID userId) {
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(userId);
    }

    public long unreadCount(UUID userId) {
        String key = unreadKey(userId);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return Long.parseLong(cached);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable reading unread count for {}, falling back to DB: {}", userId, e.getMessage());
        }

        long count = notificationRepository.countByRecipient_IdAndReadFalse(userId);
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(count), UNREAD_COUNT_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable caching unread count for {}: {}", userId, e.getMessage());
        }
        return count;
    }

    @Transactional
    public void markRead(UUID notificationId, UUID requestingUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));
        if (!notification.isRead() && notification.getRecipient().getId().equals(requestingUserId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
            bumpUnreadCache(requestingUserId, -1);
        }
    }

    @Transactional
    public void markAllRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByRecipient_IdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        try {
            redisTemplate.opsForValue().set(unreadKey(userId), "0", UNREAD_COUNT_TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable resetting unread count for {}: {}", userId, e.getMessage());
        }
    }

    /** Adjusts the cached count in place if it exists — leaves a cold cache cold, since the next read recomputes it anyway. */
    private void bumpUnreadCache(UUID userId, long delta) {
        try {
            String key = unreadKey(userId);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                Long updated = redisTemplate.opsForValue().increment(key, delta);
                if (updated != null && updated < 0) {
                    redisTemplate.opsForValue().set(key, "0", UNREAD_COUNT_TTL);
                }
            }
        } catch (Exception e) {
            log.warn("Redis unavailable updating unread count for {}: {}", userId, e.getMessage());
        }
    }

    private String unreadKey(UUID userId) {
        return "notif:unread:" + userId;
    }
}
