package com.lakshya.cycletracker.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepository extends JpaRepository<DailyLog, Long> {
    List<DailyLog> findByUserOrderByDateDesc(AppUser user);
    List<DailyLog> findByUserAndDateBetween(AppUser user, LocalDate from, LocalDate to);
    Optional<DailyLog> findByUserAndDate(AppUser user, LocalDate date);
    Optional<DailyLog> findByIdAndUser(Long id, AppUser user);
    void deleteByUser(AppUser user);
}
