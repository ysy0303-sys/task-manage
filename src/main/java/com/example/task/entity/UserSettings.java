package com.example.task.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    // 1=Monday through 7=Sunday; null or 0 means weekly report disabled
    private Integer weeklyReportDay;

    public UserSettings() {}

    public UserSettings(Long userId) {
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getWeeklyReportDay() { return weeklyReportDay; }
    public void setWeeklyReportDay(Integer weeklyReportDay) { this.weeklyReportDay = weeklyReportDay; }
}
