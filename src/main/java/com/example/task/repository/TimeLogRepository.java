package com.example.task.repository;

import com.example.task.entity.TimeLog;
import com.example.task.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeLogRepository extends JpaRepository<TimeLog, Long> {

    @Query("SELECT t FROM TimeLog t WHERE t.user = :user AND t.status = :status ORDER BY t.startTime DESC LIMIT 1")
    Optional<TimeLog> findFirstByUserAndStatus(@Param("user") User user,
                                               @Param("status") TimeLog.TimeLogStatus status);

    List<TimeLog> findByUserOrderByCreatedAtDesc(User user);
    void deleteAllByUser(User user);

    // 按任务删除所有计时记录
    @Modifying
    @Query("DELETE FROM TimeLog t WHERE t.task.id = :taskId")
    void deleteAllByTaskId(@Param("taskId") Long taskId);

    @Query("SELECT COALESCE(SUM(t.durationSeconds), 0) FROM TimeLog t " +
           "WHERE t.user.id = :userId AND t.startTime BETWEEN :start AND :end")
    Long sumDurationByUserIdAndDateRange(@Param("userId") Long userId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM TimeLog t WHERE t.user.id = :userId AND t.status = 'FINISHED' AND t.startTime >= :todayStart")
    long countTodayByUserId(@Param("userId") Long userId, @Param("todayStart") LocalDateTime todayStart);

    @Query("SELECT COALESCE(SUM(t.durationSeconds), 0) FROM TimeLog t WHERE t.user.id = :userId AND t.status = 'FINISHED' AND t.startTime >= :todayStart")
    long sumTodayDurationByUserId(@Param("userId") Long userId, @Param("todayStart") LocalDateTime todayStart);

    @Query("SELECT COALESCE(tl.task.category, '未分类'), COALESCE(SUM(tl.durationSeconds), 0) " +
           "FROM TimeLog tl LEFT JOIN tl.task t " +
           "WHERE tl.user.id = :userId AND tl.status = 'FINISHED' " +
           "AND tl.startTime >= :start AND tl.startTime < :end " +
           "GROUP BY COALESCE(t.category, '未分类') ORDER BY SUM(tl.durationSeconds) DESC")
    List<Object[]> sumDurationByCategory(@Param("userId") Long userId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
}