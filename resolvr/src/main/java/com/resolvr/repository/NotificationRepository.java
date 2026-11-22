package com.resolvr.repository;

import com.resolvr.entity.Notification;
import com.resolvr.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);
    @Modifying
    @Query("update Notification n set n.readStatus=true where n.recipient=:recipient and n.readStatus=false")
    int markUnreadForRecipient(@org.springframework.data.repository.query.Param("recipient") User recipient);
    long countByRecipientAndReadStatusFalse(User recipient);
}
