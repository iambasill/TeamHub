package com.basilcode.emsbackend.notification.repository;

import com.basilcode.emsbackend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(UUID recipientId);
    long countByRecipient_IdAndReadFalse(UUID recipientId);
    List<Notification> findByRecipient_IdAndReadFalse(UUID recipientId);
}
