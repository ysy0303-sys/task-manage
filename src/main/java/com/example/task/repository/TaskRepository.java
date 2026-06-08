//从数据库度数据
package com.example.task.repository;

import com.example.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserId(Long userId);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND (t.startDate IS NULL OR t.startDate <= :date) AND (t.deadline IS NULL OR t.deadline >= :date OR t.completed = false)")
    List<Task> findTasksActiveOnDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT t FROM Task t WHERE t.userId = :userId AND (t.startDate IS NULL OR t.startDate <= :end) AND (t.deadline IS NULL OR t.deadline >= :start)")
    List<Task> findTasksInRange(@Param("userId") Long userId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    // 按创建时间升序（最早→最晚）
    List<Task> findByUserIdOrderByCreatedAtAsc(Long userId);

    // 按创建时间降序（最晚→最早）
    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 按自定义排序 + 创建时间降序兜底
    List<Task> findByUserIdOrderBySortOrderAscCreatedAtDesc(Long userId);
}
