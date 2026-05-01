package com.parkeas.notification_service.repository;

import com.parkeas.notification_service.entity.Notification;
import com.parkeas.notification_service.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientId(Long recipientId);

    List<Notification> findByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    int countByRecipientIdAndIsRead(Long recipientId, Boolean isRead);

    List<Notification> findByType(NotificationType type);

    List<Notification> findByRelatedId(Long relatedId);

    void deleteByNotificationId(Long notificationId);
}