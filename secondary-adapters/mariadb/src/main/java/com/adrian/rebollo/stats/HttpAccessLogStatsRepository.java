package com.adrian.rebollo.stats;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.adrian.rebollo.entity.LogStats;

@Repository
public interface HttpAccessLogStatsRepository extends JpaRepository<LogStats, UUID> {

	Page<LogStats> findByInsertTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
