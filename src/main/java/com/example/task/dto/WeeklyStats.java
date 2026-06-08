package com.example.task.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record WeeklyStats(
    LocalDate weekStart,
    LocalDate weekEnd,
    int totalTasks,
    int completedTasks,
    double completionRate,
    int habitTasks,
    int habitCheckInDays,
    double habitRate,
    long totalFocusMinutes,
    int overdueTasks,
    Map<String, Integer> categoryBreakdown,
    List<String> topCompleted
) {}
