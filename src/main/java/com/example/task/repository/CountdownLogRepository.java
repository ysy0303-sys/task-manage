package com.example.task.repository;

import com.example.task.entity.CountdownLog;
import com.example.task.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountdownLogRepository extends JpaRepository<CountdownLog, Long> {

    @Query("SELECT c FROM CountdownLog c WHERE c.user = :user AND c.status = :status ORDER BY c.startTime DESC LIMIT 1")
    Optional<CountdownLog> findFirstByUserAndStatus(@Param("user") User user,
                                                    @Param("status") CountdownLog.CountdownStatus status);

    List<CountdownLog> findByUserOrderByCreatedAtDesc(User user);
    void deleteAllByUser(User user);
}