package com.example.task.service;

import com.example.task.entity.Task;
import com.example.task.entity.TimeLog;
import com.example.task.entity.User;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.TimeLogRepository;
import com.example.task.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimeLogService {

    @Autowired
    private TimeLogRepository timeLogRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public TimeLog startTimer(Long userId, String eventDescription) {
        return startTimer(userId, eventDescription, null);
    }

    @Transactional
    public TimeLog startTimer(Long userId, String eventDescription, Long taskId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        timeLogRepository.findFirstByUserAndStatus(user, TimeLog.TimeLogStatus.ACTIVE)
                .ifPresent(t -> {
                    t.setStatus(TimeLog.TimeLogStatus.CANCELLED);
                    t.setEndTime(LocalDateTime.now());
                    long seconds = Duration.between(t.getStartTime(), LocalDateTime.now()).getSeconds();
                    t.setDurationSeconds((int) seconds);
                    timeLogRepository.save(t);
                });

        TimeLog timeLog = new TimeLog(user, eventDescription);
        if (taskId != null) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));
            timeLog.setTask(task);
        }
        return timeLogRepository.save(timeLog);
    }

    @Transactional
    public TimeLog stopTimer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        TimeLog activeLog = timeLogRepository.findFirstByUserAndStatus(user, TimeLog.TimeLogStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("没有进行中的计时"));

        LocalDateTime endTime = LocalDateTime.now();
        activeLog.setEndTime(endTime);
        activeLog.setStatus(TimeLog.TimeLogStatus.FINISHED);
        long seconds = Duration.between(activeLog.getStartTime(), endTime).getSeconds();
        activeLog.setDurationSeconds((int) seconds);

        TimeLog saved = timeLogRepository.save(activeLog);

        // 累加计时到关联任务
        Task associatedTask = saved.getTask();
        if (associatedTask != null) {
            int currentTotal = associatedTask.getTotalDuration() != null ? associatedTask.getTotalDuration() : 0;
            associatedTask.setTotalDuration(currentTotal + (int) seconds);
            taskRepository.save(associatedTask);
        }

        return saved;
    }

    public TimeLog getActiveTimer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return timeLogRepository.findFirstByUserAndStatus(user, TimeLog.TimeLogStatus.ACTIVE)
                .orElse(null);
    }

    public List<TimeLog> getHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return timeLogRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void deleteAllByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        timeLogRepository.deleteAllByUser(user);
    }

    /**
     * Get focus duration distribution by category for a date range.
     * Returns list of [category, totalSeconds] pairs.
     */
    public List<Object[]> getCategoryDistribution(Long userId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return timeLogRepository.sumDurationByCategory(userId, start, end);
    }

    /**
     * Get today's focus stats for a user: session count and total duration in seconds.
     */
    public java.util.Map<String, Long> getTodayStats(Long userId) {
        java.time.LocalDateTime todayStart = java.time.LocalDate.now().atStartOfDay();
        long count = timeLogRepository.countTodayByUserId(userId, todayStart);
        long totalSeconds = timeLogRepository.sumTodayDurationByUserId(userId, todayStart);
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("count", count);
        stats.put("totalSeconds", totalSeconds);
        return stats;
    }
}