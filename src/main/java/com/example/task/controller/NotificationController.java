package com.example.task.controller;

import com.example.task.entity.Notification;
import com.example.task.entity.User;
import com.example.task.service.NotificationService;
import com.example.task.service.WeeklyReportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private WeeklyReportService weeklyReportService;

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        List<Notification> notifications = notificationService.getNotifications(user.getId());
        result.put("code", 200);
        result.put("data", notifications);
        return result;
    }

    @GetMapping("/unread-count")
    @ResponseBody
    public Map<String, Object> unreadCount(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            return result;
        }
        int count = notificationService.getUnreadCount(user.getId());
        result.put("code", 200);
        result.put("data", count);
        return result;
    }

    @PostMapping("/read/{id}")
    @ResponseBody
    public Map<String, Object> markRead(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            return result;
        }
        notificationService.markAsRead(id);
        result.put("code", 200);
        return result;
    }

    @PostMapping("/read-all")
    @ResponseBody
    public Map<String, Object> markAllRead(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            return result;
        }
        notificationService.markAllAsRead(user.getId());
        result.put("code", 200);
        return result;
    }

    @PostMapping("/check")
    @ResponseBody
    public Map<String, Object> checkDeadlines(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            return result;
        }
        notificationService.checkDeadlineReminders();
        result.put("code", 200);
        result.put("msg", "检查完成");
        return result;
    }

    @PostMapping("/generate-report")
    @ResponseBody
    public Map<String, Object> generateReport(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        try {
            Notification report = weeklyReportService.generateWeeklyReport(user.getId());
            if (report == null) {
                result.put("code", 200);
                result.put("msg", "今日已生成过周报，请在消息中心查看");
                return result;
            }
            result.put("code", 200);
            result.put("msg", "周报已生成");
            result.put("data", report);
        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "生成周报失败：" + e.getMessage());
        }
        return result;
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loginUser");
    }
}
