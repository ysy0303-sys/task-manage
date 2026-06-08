package com.example.task.repository;

import com.example.task.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.isRead = false")
    int countUnreadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.taskId = :taskId AND n.type = :type AND n.createdAt > :since")
    int countRecentByTaskAndType(@Param("userId") Long userId, @Param("taskId") Long taskId,
                                  @Param("type") String type, @Param("since") LocalDateTime since);

    void deleteByTaskId(Long taskId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.type = :type AND n.createdAt >= :since")
    int countRecentByUserAndType(@Param("userId") Long userId, @Param("type") String type,
                                  @Param("since") LocalDateTime since);
}
