package com.example.task.controller;

import com.example.task.entity.Task;
import com.example.task.entity.User;
import com.example.task.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @GetMapping("/list")
    @ResponseBody
    public Map<String, Object> list(@RequestParam(required = false) String date,
                                    @RequestParam(required = false, defaultValue = "created_desc") String sortBy,
                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        List<Task> tasks;
        if ("all".equals(date)) {
            tasks = taskService.getTasksByUserSorted(user.getId(), null, sortBy);
        } else {
            LocalDate filterDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
            tasks = taskService.getTasksByUserSorted(user.getId(), filterDate, sortBy);
        }
        // 为习惯任务附带打卡数据
        List<Map<String, Object>> enrichedList = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("description", task.getDescription());
            item.put("priority", task.getPriority());
            item.put("completed", task.getCompleted());
            item.put("deadline", task.getDeadline());
            item.put("startDate", task.getStartDate());
            item.put("category", task.getCategory());
            item.put("totalDuration", task.getTotalDuration());
            item.put("userId", task.getUserId());
            item.put("taskType", task.getTaskType());
            item.put("createdAt", task.getCreatedAt());
            item.put("sortOrder", task.getSortOrder());
            if ("HABIT".equals(task.getTaskType())) {
                item.put("checkInData", taskService.getCheckInData(task.getId(), user.getId()));
            }
            enrichedList.add(item);
        }
        result.put("code", 200);
        result.put("msg", "成功");
        result.put("data", enrichedList);
        return result;
    }

    @PostMapping("/sort-order")
    @ResponseBody
    public Map<String, Object> updateSortOrder(@RequestBody Map<Long, Integer> sortOrders, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        taskService.updateSortOrders(sortOrders);
        result.put("code", 200);
        result.put("msg", "排序已保存");
        return result;
    }

    @PostMapping("/add")
    @ResponseBody
    public Map<String, Object> add(Task task, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        task.setUserId(user.getId());
        if (task.getCompleted() == null) {
            task.setCompleted(false);
        }
        Task saved = taskService.createTask(task);
        result.put("code", 200);
        result.put("msg", "添加成功");
        result.put("data", saved);
        return result;
    }

    @PostMapping("/update")
    @ResponseBody
    public Map<String, Object> update(Task task, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        Task existing = taskService.getTaskByIdAndUser(task.getId(), user.getId());
        if (existing == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }
        if (task.getTitle() != null) existing.setTitle(task.getTitle());
        if (task.getDescription() != null) existing.setDescription(task.getDescription());
        if (task.getPriority() != null) existing.setPriority(task.getPriority());
        if (task.getDeadline() != null) existing.setDeadline(task.getDeadline());
        if (task.getStartDate() != null) existing.setStartDate(task.getStartDate());
        if (task.getCategory() != null) existing.setCategory(task.getCategory());
        if (task.getCompleted() != null) existing.setCompleted(task.getCompleted());
        if (task.getTaskType() != null) existing.setTaskType(task.getTaskType());
        Task saved = taskService.updateTask(existing);
        result.put("code", 200);
        result.put("msg", "更新成功");
        result.put("data", saved);
        return result;
    }

    @PostMapping("/toggle/{id}")
    @ResponseBody
    public Map<String, Object> toggle(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        Task task = taskService.getTaskByIdAndUser(id, user.getId());
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }
        task.setCompleted(!Boolean.TRUE.equals(task.getCompleted()));
        taskService.updateTask(task);
        result.put("code", 200);
        result.put("msg", "操作成功");
        result.put("data", task);
        return result;
    }

    @GetMapping("/range")
    @ResponseBody
    public Map<String, Object> range(@RequestParam String start, @RequestParam String end, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        LocalDate startDate = LocalDate.parse(start);
        LocalDate endDate = LocalDate.parse(end);
        List<Task> tasks = taskService.getTasksByUserAndRange(user.getId(), startDate, endDate);
        result.put("code", 200);
        result.put("msg", "成功");
        result.put("data", tasks);
        return result;
    }

    @GetMapping("/delete/{id}")
    @ResponseBody
    public Map<String, Object> delete(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        taskService.deleteTaskByUser(id, user.getId());
        result.put("code", 200);
        result.put("msg", "已删除");
        return result;
    }

    @PostMapping("/checkin/{id}")
    @ResponseBody
    public Map<String, Object> checkIn(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        Task task = taskService.getTaskByIdAndUser(id, user.getId());
        if (task == null) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }
        if (!"HABIT".equals(task.getTaskType())) {
            result.put("code", 400);
            result.put("msg", "该任务不是习惯类型");
            return result;
        }
        boolean ok = taskService.checkIn(id, user.getId());
        if (!ok) {
            result.put("code", 400);
            result.put("msg", "今天已经打过卡了");
            return result;
        }
        Map<String, Object> checkInData = taskService.getCheckInData(id, user.getId());
        result.put("code", 200);
        result.put("msg", "打卡成功");
        result.put("data", checkInData);
        return result;
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public Map<String, Object> detail(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        Map<String, Object> detail = taskService.getTaskDetail(id, user.getId());
        if (detail.isEmpty()) {
            result.put("code", 404);
            result.put("msg", "任务不存在");
            return result;
        }
        result.put("code", 200);
        result.put("msg", "成功");
        result.put("data", detail);
        return result;
    }

    @GetMapping("/checkin/data/{id}")
    @ResponseBody
    public Map<String, Object> checkInData(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        User user = getCurrentUser(session);
        if (user == null) {
            result.put("code", 401);
            result.put("msg", "请先登录");
            return result;
        }
        result.put("code", 200);
        result.put("msg", "成功");
        result.put("data", taskService.getCheckInData(id, user.getId()));
        return result;
    }

    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loginUser");
    }
}
