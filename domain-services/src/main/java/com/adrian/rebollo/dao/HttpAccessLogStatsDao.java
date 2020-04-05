package com.adrian.rebollo.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.adrian.rebollo.model.HttpAccessLogStats;

public interface HttpAccessLogStatsDao {

	void save(HttpAccessLogStats httpAccessLogStats);

	Optional<List<HttpAccessLogStats>> findByDateBetween(LocalDateTime start, LocalDateTime end, int chunk);
}
