package com.example.task.service;

import com.example.task.entity.HabitCheckIn;
import com.example.task.entity.Task;
import com.example.task.repository.HabitCheckInRepository;
import com.example.task.repository.TaskRepository;
import com.example.task.repository.TimeLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HabitCheckInRepository habitCheckInRepository;

    @Autowired
    private TimeLogRepository timeLogRepository;

    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public List<Task> getTasksByUser(Long userId) {
        return taskRepository.findByUserId(userId);
    }

    public List<Task> getTasksByUserAndDate(Long userId, LocalDate date) {
        return taskRepository.findTasksActiveOnDate(userId, date);
    }

    // 根据排序方式获取任务列表
    public List<Task> getTasksByUserSorted(Long userId, LocalDate date, String sortBy) {
        List<Task> tasks;
        if (date != null) {
            tasks = taskRepository.findTasksActiveOnDate(userId, date);
        } else {
            tasks = taskRepository.findByUserIdOrderBySortOrderAscCreatedAtDesc(userId);
        }
        // 根据sortBy参数在内存中排序（用id作为二级排序，确保顺序明确）
        if ("created_asc".equals(sortBy)) {
            tasks.sort(java.util.Comparator.comparing(Task::getCreatedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                .thenComparing(Task::getId));
        } else if ("created_desc".equals(sortBy)) {
            tasks.sort(java.util.Comparator.comparing(Task::getCreatedAt,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                .thenComparing(java.util.Comparator.comparing(Task::getId).reversed()));
        } else if ("deadline_asc".equals(sortBy)) {
            tasks.sort(java.util.Comparator.comparing(Task::getDeadline,
                java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
                .thenComparing(Task::getId));
        } else if ("deadline_desc".equals(sortBy)) {
            tasks.sort(java.util.Comparator.comparing(Task::getDeadline,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                .thenComparing(java.util.Comparator.comparing(Task::getId).reversed()));
        }
        // "custom" 保持数据库返回顺序（sortOrder ASC, createdAt DESC）
        return tasks;
    }

    // 批量更新排序顺序（拖拽后保存）
    public void updateSortOrders(java.util.Map<Long, Integer> taskSortOrders) {
        for (java.util.Map.Entry<Long, Integer> entry : taskSortOrders.entrySet()) {
            taskRepository.findById(entry.getKey()).ifPresent(task -> {
                task.setSortOrder(entry.getValue());
                taskRepository.save(task);
            });
        }
    }

    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteTaskByUser(Long id, Long userId) {
        taskRepository.findByIdAndUserId(id, userId)
                .ifPresent(task -> {
                    // 删除关联的计时记录
                    timeLogRepository.deleteAllByTaskId(id);
                    // 删除关联的打卡记录
                    habitCheckInRepository.findByTaskIdAndUserIdOrderByCheckInDateDesc(id, userId)
                            .forEach(habitCheckInRepository::delete);
                    taskRepository.delete(task);
                });
    }

    public Task updateTask(Task task) {
        return taskRepository.save(task);
    }

    public List<Task> getTasksByUserAndRange(Long userId, LocalDate start, LocalDate end) {
        return taskRepository.findTasksInRange(userId, start, end);
    }

    public Task getTaskByIdAndUser(Long id, Long userId) {
        return taskRepository.findByIdAndUserId(id, userId).orElse(null);
    }

    // ========== 习惯打卡 ==========

    public boolean checkIn(Long taskId, Long userId) {
        LocalDate today = LocalDate.now();
        if (habitCheckInRepository.findByTaskIdAndUserIdAndCheckInDate(taskId, userId, today).isPresent()) {
            return false; // 今日已打卡
        }
        HabitCheckIn checkIn = new HabitCheckIn(taskId, userId, today);
        habitCheckInRepository.save(checkIn);
        return true;
    }

    public Map<String, Object> getCheckInData(Long taskId, Long userId) {
        Map<String, Object> data = new HashMap<>();
        List<HabitCheckIn> records = habitCheckInRepository.findByTaskIdAndUserIdOrderByCheckInDateDesc(taskId, userId);
        long totalCount = records.size();

        // 计算连续打卡天数
        int streak = calcStreak(records);

        // 今日是否已打卡
        boolean todayCheckedIn = !records.isEmpty() && records.get(0).getCheckInDate().equals(LocalDate.now());

        data.put("totalCount", totalCount);
        data.put("streak", streak);
        data.put("todayCheckedIn", todayCheckedIn);
        data.put("records", records);
        return data;
    }

    private int calcStreak(List<HabitCheckIn> records) {
        if (records.isEmpty()) return 0;
        int streak = 0;
        // 从最近一次打卡日期开始往前数，而不是从今天
        LocalDate expected = records.get(0).getCheckInDate();
        for (HabitCheckIn r : records) {
            LocalDate d = r.getCheckInDate();
            if (d.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (d.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }

    public Map<String, Object> getTaskDetail(Long taskId, Long userId) {
        Map<String, Object> detail = new HashMap<>();
        Task task = taskRepository.findByIdAndUserId(taskId, userId).orElse(null);
        if (task == null) return detail;

        detail.put("id", task.getId());
        detail.put("title", task.getTitle());
        detail.put("category", task.getCategory());
        detail.put("startDate", task.getStartDate());
        detail.put("deadline", task.getDeadline());
        detail.put("completed", task.getCompleted());
        detail.put("taskType", task.getTaskType());
        detail.put("totalDuration", task.getTotalDuration());

        // 计算天数相关
        LocalDate today = LocalDate.now();
        if (task.getDeadline() != null) {
            long daysRemaining = today.until(task.getDeadline()).getDays();
            detail.put("daysRemaining", Math.max(0, daysRemaining));
            detail.put("isOverdue", daysRemaining < 0);
        }
        if (task.getStartDate() != null) {
            long daysSinceStart = task.getStartDate().until(today).getDays();
            detail.put("daysSinceStart", Math.max(0, daysSinceStart));
            if (task.getDeadline() != null && task.getStartDate() != null) {
                long totalDays = task.getStartDate().until(task.getDeadline()).getDays();
                detail.put("totalDays", Math.max(1, totalDays));
            }
        }

        // 习惯打卡数据
        if ("HABIT".equals(task.getTaskType())) {
            Map<String, Object> checkInData = getCheckInData(taskId, userId);
            detail.put("checkInData", checkInData);
            // 最近30天打卡日历
            List<Map<String, Object>> calendar = new ArrayList<>();
            for (int i = 29; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                Map<String, Object> day = new HashMap<>();
                day.put("date", date.toString());
                day.put("dayOfWeek", date.getDayOfWeek().getValue());
                @SuppressWarnings("unchecked")
                List<HabitCheckIn> records = (List<HabitCheckIn>) checkInData.get("records");
                boolean checked = false;
                if (records != null) {
                    for (HabitCheckIn r : records) {
                        if (r.getCheckInDate().equals(date)) {
                            checked = true;
                            break;
                        }
                    }
                }
                day.put("checked", checked);
                calendar.add(day);
            }
            detail.put("calendar", calendar);
        }

        return detail;
    }
}
