package com.example.task.service;

import com.example.task.entity.CountdownLog;
import com.example.task.entity.User;
import com.example.task.repository.CountdownLogRepository;
import com.example.task.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CountdownService {

    @Autowired
    private CountdownLogRepository countdownLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public CountdownLog startCountdown(Long userId, Integer targetDurationSeconds, String eventDescription) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        countdownLogRepository.findFirstByUserAndStatus(user, CountdownLog.CountdownStatus.ACTIVE)
                .ifPresent(c -> {
                    c.setStatus(CountdownLog.CountdownStatus.CANCELLED);
                    c.setEndTime(LocalDateTime.now());
                    long seconds = Duration.between(c.getStartTime(), LocalDateTime.now()).getSeconds();
                    c.setActualDurationSeconds((int) seconds);
                    countdownLogRepository.save(c);
                });

        CountdownLog countdownLog = new CountdownLog(user, targetDurationSeconds, eventDescription);
        return countdownLogRepository.save(countdownLog);
    }

    @Transactional
    public CountdownLog completeCountdown(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        CountdownLog activeLog = countdownLogRepository.findFirstByUserAndStatus(user, CountdownLog.CountdownStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("没有进行中的倒计时"));

        LocalDateTime endTime = LocalDateTime.now();
        activeLog.setEndTime(endTime);
        activeLog.setStatus(CountdownLog.CountdownStatus.COMPLETED);
        long seconds = Duration.between(activeLog.getStartTime(), endTime).getSeconds();
        activeLog.setActualDurationSeconds((int) seconds);

        return countdownLogRepository.save(activeLog);
    }

    @Transactional
    public CountdownLog cancelCountdown(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        CountdownLog activeLog = countdownLogRepository.findFirstByUserAndStatus(user, CountdownLog.CountdownStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("没有进行中的倒计时"));

        LocalDateTime endTime = LocalDateTime.now();
        activeLog.setEndTime(endTime);
        activeLog.setStatus(CountdownLog.CountdownStatus.CANCELLED);
        long seconds = Duration.between(activeLog.getStartTime(), endTime).getSeconds();
        activeLog.setActualDurationSeconds((int) seconds);

        return countdownLogRepository.save(activeLog);
    }

    public List<CountdownLog> getHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return countdownLogRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void deleteAllByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        countdownLogRepository.deleteAllByUser(user);
    }
}