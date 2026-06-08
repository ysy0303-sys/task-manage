package com.example.task.scheduler;

import com.example.task.entity.UserSettings;
import com.example.task.repository.UserSettingsRepository;
import com.example.task.service.WeeklyReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class WeeklyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeeklyReportScheduler.class);

    @Autowired
    private WeeklyReportService weeklyReportService;

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    // Runs daily at 9:00 AM
    @Scheduled(cron = "0 0 9 * * *")
    public void generateWeeklyReports() {
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue(); // 1=Mon..7=Sun
        log.info("Weekly report scheduler running for dayOfWeek={}", dayOfWeek);

        List<UserSettings> settings = userSettingsRepository.findByWeeklyReportDay(dayOfWeek);
        log.info("Found {} users with weekly report scheduled for today", settings.size());

        for (UserSettings setting : settings) {
            try {
                weeklyReportService.generateWeeklyReport(setting.getUserId());
            } catch (Exception e) {
                log.error("Failed to generate weekly report for userId={}: {}", setting.getUserId(), e.getMessage());
            }
        }
    }
}
