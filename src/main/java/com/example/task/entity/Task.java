package com.example.task.entity;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity  
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;        // 任务标题
    private String description;  // 任务描述

    private Integer priority;    // 优先级：1高 2中 3低

    private Boolean completed;   // 是否完成

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline; // 截止时间

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate; // 开始日期

    private String category;     // 任务类型：学习/运动/生活/阅读/其他/自定义

    private Integer totalDuration = 0;  // 累计计时（秒），多次计时求和

    private String taskType = "TODO";  // 任务类型：TODO=待办, HABIT=习惯养成

    private LocalDateTime createdAt = LocalDateTime.now();  // 创建时间（精确到秒）

    private Integer sortOrder;  // 自定义排序序号（拖拽排序用）

    private Long userId;        // 所属用户（关键！）

    // getter & setter
    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}