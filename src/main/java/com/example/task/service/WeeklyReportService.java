package com.example.task.service;

import com.example.task.dto.WeeklyStats;
import com.example.task.entity.*;
import com.example.task.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeeklyReportService {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportService.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HabitCheckInRepository habitCheckInRepository;

    @Autowired
    private TimeLogRepository timeLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AiReportService aiReportService;

    /**
     * Generate a weekly report for the given user.
     * Returns the created Notification, or null if a report already exists for today.
     */
    @Transactional
    public Notification generateWeeklyReport(Long userId) {
        // Dedup: check if report already generated today
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        int existingCount = notificationRepository.countRecentByUserAndType(
                userId, "WEEKLY_REPORT", todayStart);
        if (existingCount > 0) {
            log.info("Weekly report already exists for userId={} today, skipping", userId);
            return null;
        }

        // Collect weekly statistics
        WeeklyStats stats = collectWeeklyStats(userId);

        // Generate report text (AI or fallback)
        String reportText = aiReportService.generateReport(stats);

        // Create notification
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTaskId(null);
        notification.setTaskTitle("本周工作总结");
        notification.setMessage(reportText);
        notification.setType("WEEKLY_REPORT");
        notification.setIsRead(false);

        notification = notificationRepository.save(notification);
        log.info("Generated weekly report for userId={}", userId);
        return notification;
    }

    /**
     * Collect all statistics for the past 7 days for a user.
     */
    private WeeklyStats collectWeeklyStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // If today is before Monday of this week (shouldn't happen normally), use last 7 days
        if (weekStart.isAfter(today)) {
            weekStart = today.minusDays(6);
            weekEnd = today;
        }

        // Query tasks in range
        List<Task> weekTasks = taskRepository.findTasksInRange(userId, weekStart, weekEnd);

        int totalTasks = weekTasks.size();
        int completedTasks = (int) weekTasks.stream().filter(Task::getCompleted).count();
        double completionRate = totalTasks > 0 ? (double) completedTasks / totalTasks : 0.0;

        // Habit tasks
        List<Task> habitTasks = weekTasks.stream()
                .filter(t -> "HABIT".equals(t.getTaskType()))
                .toList();
        int habitTaskCount = habitTasks.size();

        // Check-in days in the week
        long checkInDays = habitCheckInRepository.countByUserIdAndCheckInDateBetween(userId, weekStart, weekEnd);
        double habitRate = habitTaskCount > 0 ? (double) checkInDays / 7 : 0.0;

        // Focus time (TimeLog)
        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = weekEnd.atTime(LocalTime.MAX);
        Long totalSeconds = timeLogRepository.sumDurationByUserIdAndDateRange(userId, startDateTime, endDateTime);
        long totalFocusMinutes = totalSeconds != null ? totalSeconds / 60 : 0;

        // Overdue tasks (deadline passed but not completed)
        int overdueTasks = (int) weekTasks.stream()
                .filter(t -> !t.getCompleted() && t.getDeadline() != null && t.getDeadline().isBefore(today))
                .count();

        // Category breakdown
        Map<String, Integer> categoryBreakdown = weekTasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCategory() != null ? t.getCategory() : "未分类",
                        Collectors.summingInt(t -> 1)
                ));

        // Top completed tasks
        List<String> topCompleted = weekTasks.stream()
                .filter(Task::getCompleted)
                .map(Task::getTitle)
                .limit(5)
                .collect(Collectors.toList());

        return new WeeklyStats(
                weekStart, weekEnd,
                totalTasks, completedTasks, completionRate,
                habitTaskCount, (int) checkInDays, habitRate,
                totalFocusMinutes, overdueTasks,
                categoryBreakdown, topCompleted
        );
    }
}
