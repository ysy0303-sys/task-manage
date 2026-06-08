package com.example.task.timer;

import com.example.task.entity.TimeLog;
import com.example.task.entity.User;
import com.example.task.service.TimeLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/timer")
public class TimerController {

    @Autowired
    private TimeLogService timeLogService;

    @PostMapping("/start")
    public ResponseEntity<TimeLog> startTimer(@RequestParam String event,
                                              @RequestParam(required = false) Long taskId,
                                              HttpSession session) {
        Long userId = getCurrentUserId(session);
        TimeLog timeLog = timeLogService.startTimer(userId, event, taskId);
        return ResponseEntity.ok(timeLog);
    }

    @PostMapping("/stop")
    public ResponseEntity<TimeLog> stopTimer(HttpSession session) {
        Long userId = getCurrentUserId(session);
        TimeLog timeLog = timeLogService.stopTimer(userId);
        return ResponseEntity.ok(timeLog);
    }

    @GetMapping("/active")
    public ResponseEntity<TimeLog> getActive(HttpSession session) {
        Long userId = getCurrentUserId(session);
        TimeLog active = timeLogService.getActiveTimer(userId);
        return ResponseEntity.ok(active);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TimeLog>> getHistory(HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<TimeLog> logs = timeLogService.getHistory(userId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/today-stats")
    public ResponseEntity<java.util.Map<String, Long>> getTodayStats(HttpSession session) {
        Long userId = getCurrentUserId(session);
        java.util.Map<String, Long> stats = timeLogService.getTodayStats(userId);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(HttpSession session) {
        Long userId = getCurrentUserId(session);
        timeLogService.deleteAllByUser(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Object>> getDistribution(
            @RequestParam(defaultValue = "day") String range,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        Long userId = getCurrentUserId(session);

        LocalDate today = LocalDate.now();
        LocalDateTime start, end;

        switch (range) {
            case "week":
                start = today.minusDays(6).atStartOfDay();
                end = today.plusDays(1).atStartOfDay();
                break;
            case "month":
                start = today.withDayOfMonth(1).atStartOfDay();
                end = today.plusDays(1).atStartOfDay();
                break;
            case "custom":
                if (startDate != null && endDate != null) {
                    start = LocalDate.parse(startDate).atStartOfDay();
                    end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
                } else {
                    start = today.atStartOfDay();
                    end = today.plusDays(1).atStartOfDay();
                }
                break;
            default: // day
                start = today.atStartOfDay();
                end = today.plusDays(1).atStartOfDay();
                break;
        }

        List<Object[]> rows = timeLogService.getCategoryDistribution(userId, start, end);
        List<Map<String, Object>> items = new ArrayList<>();
        long totalSeconds = 0;
        List<Map<String, Object>> rawItems = new ArrayList<>();

        for (Object[] row : rows) {
            String cat = (String) row[0];
            long sec = ((Number) row[1]).longValue();
            if (sec > 0) {
                rawItems.add(Map.of("category", cat, "seconds", sec));
                totalSeconds += sec;
            }
        }

        for (Map<String, Object> item : rawItems) {
            long sec = (long) item.get("seconds");
            double pct = totalSeconds > 0 ? Math.round((double) sec / totalSeconds * 1000.0) / 10.0 : 0;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("category", item.get("category"));
            entry.put("seconds", sec);
            entry.put("minutes", sec / 60);
            entry.put("percentage", pct);
            items.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("totalSeconds", totalSeconds);
        result.put("totalMinutes", totalSeconds / 60);
        return ResponseEntity.ok(result);
    }

    private Long getCurrentUserId(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user.getId();
    }
}
