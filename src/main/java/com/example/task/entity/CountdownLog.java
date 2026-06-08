package com.example.task.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "countdown_log")
public class CountdownLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore // 避免序列化整个 User 对象
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "target_duration_seconds", nullable = false)
    private Integer targetDurationSeconds;

    @Column(name = "event_description")
    private String eventDescription;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "actual_duration_seconds")
    private Integer actualDurationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CountdownStatus status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 状态枚举
    public enum CountdownStatus {
        ACTIVE,     // 进行中
        COMPLETED,  // 正常完成
        CANCELLED   // 手动取消
    }

    // 无参构造器（JPA 必需）
    public CountdownLog() {
    }

    // 便捷构造器（开始倒计时时使用）
    public CountdownLog(User user, Integer targetDurationSeconds, String eventDescription) {
        this.user = user;
        this.targetDurationSeconds = targetDurationSeconds;
        this.eventDescription = eventDescription;
        this.startTime = LocalDateTime.now();
        this.status = CountdownStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    // --- 为前端提供的额外 getter（避免暴露完整 User 对象）---
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getUserName() {
        return user != null ? user.getUsername() : null;
    }

    // --- 标准 Getter & Setter ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getTargetDurationSeconds() {
        return targetDurationSeconds;
    }

    public void setTargetDurationSeconds(Integer targetDurationSeconds) {
        this.targetDurationSeconds = targetDurationSeconds;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
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

    public Integer getActualDurationSeconds() {
        return actualDurationSeconds;
    }

    public void setActualDurationSeconds(Integer actualDurationSeconds) {
        this.actualDurationSeconds = actualDurationSeconds;
    }

    public CountdownStatus getStatus() {
        return status;
    }

    public void setStatus(CountdownStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}