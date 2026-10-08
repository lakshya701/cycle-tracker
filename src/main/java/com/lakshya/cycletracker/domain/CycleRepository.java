package com.lakshya.cycletracker.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CycleRepository extends JpaRepository<Cycle, Long> {
    List<Cycle> findByUserOrderByStartDateDesc(AppUser user);
    List<Cycle> findByUserOrderByStartDateAsc(AppUser user);
    Optional<Cycle> findByIdAndUser(Long id, AppUser user);
    Optional<Cycle> findFirstByUserOrderByStartDateDesc(AppUser user);
    void deleteByUser(AppUser user);
}
