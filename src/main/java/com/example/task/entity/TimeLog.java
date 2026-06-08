package com.example.task.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "time_log")
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = true)
    private Task task;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TimeLogStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "event_description")
    private String eventDescription;

    // 枚举定义
    public enum TimeLogStatus {
        ACTIVE, FINISHED, CANCELLED
    }

    // 无参构造器
    public TimeLog() {
    }

    // 简便构造器（保留原有，如果你不再使用可以删除，建议保留以备后用）
    public TimeLog(Task task, User user) {
        this.task = task;
        this.user = user;
        this.startTime = LocalDateTime.now();
        this.status = TimeLogStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // 新构造器（用于正计时）
    public TimeLog(User user, String eventDescription) {
        this.user = user;
        this.eventDescription = eventDescription;
        this.startTime = LocalDateTime.now();
        this.status = TimeLogStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // --- 为前端提供的额外 getter ---
    public Long getTaskId() {
        return task != null ? task.getId() : null;
    }

    public String getTaskName() {
        return task != null ? task.getTitle() : null;
    }

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getUserName() {
        return user != null ? user.getUsername() : null;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    // --- 原有 Getter/Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public TimeLogStatus getStatus() {
        return status;
    }

    public void setStatus(TimeLogStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}