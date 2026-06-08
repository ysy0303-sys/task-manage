package com.example.task.repository;

import com.example.task.entity.HabitCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HabitCheckInRepository extends JpaRepository<HabitCheckIn, Long> {

    List<HabitCheckIn> findByTaskIdAndUserIdOrderByCheckInDateDesc(Long taskId, Long userId);

    Optional<HabitCheckIn> findByTaskIdAndUserIdAndCheckInDate(Long taskId, Long userId, LocalDate date);

    long countByTaskIdAndUserId(Long taskId, Long userId);

    @Query("SELECT h FROM HabitCheckIn h WHERE h.userId = :userId AND h.checkInDate BETWEEN :start AND :end")
    List<HabitCheckIn> findByUserIdAndCheckInDateBetween(@Param("userId") Long userId,
                                                          @Param("start") LocalDate start,
                                                          @Param("end") LocalDate end);

    long countByUserIdAndCheckInDateBetween(Long userId, LocalDate start, LocalDate end);
}
