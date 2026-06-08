package com.example.task.timer;

import com.example.task.entity.CountdownLog;
import com.example.task.entity.User;
import com.example.task.service.CountdownService;
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

import java.util.List;

@RestController
@RequestMapping("/api/countdown")
public class CountdownController {

    @Autowired
    private CountdownService countdownService;

    @PostMapping("/start")
    public ResponseEntity<CountdownLog> startCountdown(@RequestParam Integer targetSeconds,
                                                       @RequestParam(required = false) String event,
                                                       HttpSession session) {
        Long userId = getCurrentUserId(session);
        CountdownLog log = countdownService.startCountdown(userId, targetSeconds, event);
        return ResponseEntity.ok(log);
    }

    @PostMapping("/complete")
    public ResponseEntity<CountdownLog> completeCountdown(HttpSession session) {
        Long userId = getCurrentUserId(session);
        CountdownLog log = countdownService.completeCountdown(userId);
        return ResponseEntity.ok(log);
    }

    @PostMapping("/cancel")
    public ResponseEntity<CountdownLog> cancelCountdown(HttpSession session) {
        Long userId = getCurrentUserId(session);
        CountdownLog log = countdownService.cancelCountdown(userId);
        return ResponseEntity.ok(log);
    }

    @GetMapping("/history")
    public ResponseEntity<List<CountdownLog>> getHistory(HttpSession session) {
        Long userId = getCurrentUserId(session);
        List<CountdownLog> logs = countdownService.getHistory(userId);
        return ResponseEntity.ok(logs);
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearHistory(HttpSession session) {
        Long userId = getCurrentUserId(session);
        countdownService.deleteAllByUser(userId);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        return user.getId();
    }
}
