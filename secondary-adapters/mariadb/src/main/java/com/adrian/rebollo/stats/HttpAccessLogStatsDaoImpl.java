package com.adrian.rebollo.stats;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.dao.HttpAccessLogStatsDao;
import com.adrian.rebollo.entity.LogStats;
import com.adrian.rebollo.mapper.HttpAccessLogStatsMapper;
import com.adrian.rebollo.model.HttpAccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpAccessLogStatsDaoImpl implements HttpAccessLogStatsDao {

	private final HttpAccessLogStatsMapper mapper;
	private final HttpAccessLogStatsRepository repository;

	@Override
	public void save(final HttpAccessLogStats httpAccessLogStats) {
		LogStats logStats = mapper.toEntity(httpAccessLogStats);
		repository.save(logStats);
		LOG.debug("Proceeded to save logStats={}", logStats);
	}

	@Override
	public Optional<List<HttpAccessLogStats>> findByDateBetween(final LocalDateTime start, final LocalDateTime end, final int chunkSize) {

		LOG.info("Proceeding to find logs stats by date range from={}, to={}, chunk={}.", start, end, chunkSize);

		final Pageable pageable = PageRequest.of(0, chunkSize, Sort.by("insertTime").ascending());
		final Page<LogStats> logPage = repository.findByInsertTimeBetween(start, end, pageable);

		if (logPage.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(logPage
				.get()
				.map(mapper::toDomain)
				.collect(Collectors.toList()));
	}
}
