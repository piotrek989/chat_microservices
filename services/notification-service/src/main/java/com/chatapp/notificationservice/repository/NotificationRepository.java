package com.chatapp.notificationservice.repository;

import com.chatapp.notificationservice.entity.Notification;
import com.chatapp.notificationservice.entity.NotificationType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
	List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    List<Notification> findByTypeAndUserIdOrderByCreatedAtDesc(NotificationType type, String userId);
}
