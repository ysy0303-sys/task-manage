package com.example.task.scheduler;

import com.example.task.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    @Autowired
    private NotificationService notificationService;

    // 每小时检查一次
    @Scheduled(fixedRate = 3600000)
    public void checkDeadlines() {
        notificationService.checkDeadlineReminders();
    }
}
