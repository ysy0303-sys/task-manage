package com.example.task.service;

import com.example.task.entity.Notification;
import com.example.task.entity.Task;
import com.example.task.repository.NotificationRepository;
import com.example.task.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TaskRepository taskRepository;

    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public int getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    /**
     * 检查所有未完成任务，对截止日期在 24 小时内的发送提醒。
     * 每个任务只提醒一次（24h内不重复）。
     */
    @Transactional
    public void checkDeadlineReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Task> allTasks = taskRepository.findAll();
        for (Task task : allTasks) {
            if (Boolean.TRUE.equals(task.getCompleted())) continue;
            if (task.getDeadline() == null) continue;

            // 截止日期在今天或明天
            boolean isApproaching = !task.getDeadline().isBefore(today) && !task.getDeadline().isAfter(tomorrow);

            if (!isApproaching) continue;

            // 24h 内不重复提醒
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            int count = notificationRepository.countRecentByTaskAndType(
                    task.getUserId(), task.getId(), "DEADLINE_REMINDER", since);
            if (count > 0) continue;

            String urgency = task.getDeadline().isEqual(today) ? "今天" : "明天";
            String message = "任务「" + task.getTitle() + "」将在" + urgency + "截止，请注意完成！";

            Notification notification = new Notification();
            notification.setUserId(task.getUserId());
            notification.setTaskId(task.getId());
            notification.setTaskTitle(task.getTitle());
            notification.setMessage(message);
            notification.setType("DEADLINE_REMINDER");
            notification.setIsRead(false);

            notificationRepository.save(notification);
        }
    }
}
