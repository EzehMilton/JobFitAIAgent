package com.milton.agent.repository;

import com.milton.agent.models.DashboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DashboardEntryRepository extends JpaRepository<DashboardEntry, Long> {

    List<DashboardEntry> findByUserIdOrderByIdDesc(Long userId);

    long countByUserId(Long userId);
}
