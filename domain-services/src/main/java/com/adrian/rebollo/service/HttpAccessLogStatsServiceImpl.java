package com.adrian.rebollo.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.dao.HttpAccessLogLineDao;
import com.adrian.rebollo.dao.HttpAccessLogStatsDao;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpAccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpAccessLogStatsServiceImpl {

	private final InternalDispatcher internalDispatcher;
	private final HttpAccessLogLineDao httpAccessLogLineDao;
	private final HttpAccessLogStatsDao httpAccessLogStatsDao;
	private final HttpAccessLogStatsComponent httpAccessLogStatsComponent;

	@Value("${service.schedulers.stats.delay}")
	private int schedulerDelay;
	@Value("${service.schedulers.stats.chunk}")
	private int chunk;

	/**
	 * Synchronize it as this is giving to the service state, so avoid be accessed by more than one thread.
	 */
	private AtomicLong lastSeqId = new AtomicLong(Long.MIN_VALUE);

	/**
	 * This scheduler triggers Log Stats aggregations.
	 * Triggered each (default) 10 seconds.
	 * This aggregates log lines, compute them, and dispatch through internal Queue Message Broker
	 */
	@ConditionalOnProperty( "service.schedulers.stats.enabled" )
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Scheduled(fixedDelayString = "${service.schedulers.stats.delay}", initialDelayString = "${service.schedulers.stats.delay}")
	public void aggregate() {

		final LocalDateTime end = LocalDateTime.now();
		final LocalDateTime start = end.minus(schedulerDelay, ChronoUnit.MILLIS);

		LOG.info("Triggered scheduler to aggregate logs statistics lastSecId={}.", lastSeqId);

		/*
		 * Notice this triggers the last 10sec added Log Lines.
		 * ie:
		 * 1st - from seqId > 0
		 * 2nd - from seqId > MAX 1st scheduler seqId
		 * 3rd - from seqId > MAX 2nd scheduler seqId
		 * ...
		 */
		final Optional<List<HttpAccessLogLine>> optionalLogs = httpAccessLogLineDao.findBySeqIdGreater(lastSeqId.get(), chunk);

		HttpAccessLogStats httpAccessLogStats = new HttpAccessLogStats(start, end);

		if(optionalLogs.isPresent() && !optionalLogs.get().isEmpty()) {
			final List<HttpAccessLogLine> logs = optionalLogs.get();

			httpAccessLogStats = aggregateLogs(logs);

			LOG.info("Finished aggregation httpAccessLogStats={}.", httpAccessLogStats);

			httpAccessLogStatsDao.save(httpAccessLogStats);

			//last sequence Id is the max seqId from the retrieved result list stats.
			saveLastSeqId(logs.stream().map(HttpAccessLogLine::getSeqId).max(Long::compare).orElse(Long.MIN_VALUE));
		}

		//always dispatch the stats.
		internalDispatcher.dispatch(httpAccessLogStats);
	}

	private HttpAccessLogStats aggregateLogs(final List<HttpAccessLogLine> logs) {

		final HttpAccessLogStats httpAccessLogStats = new HttpAccessLogStats();

		//get min and max date in the same loop iteration (not double loop to get both)
		final LongSummaryStatistics instantLogStatistics = logs.stream()
				.map(HttpAccessLogLine::getInsertTime)
				.map((date) -> date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
				.mapToLong(Long::new)
				.summaryStatistics();
		httpAccessLogStats.setStart(Instant.ofEpochMilli(instantLogStatistics.getMin()).atZone(ZoneId.systemDefault()).toLocalDateTime());
		httpAccessLogStats.setEnd(Instant.ofEpochMilli(instantLogStatistics.getMax()).atZone(ZoneId.systemDefault()).toLocalDateTime());

		LOG.info("Started aggregating {} log lines data lastSeqId={}.", logs.size(), lastSeqId);

		//compute every single Log Line independently.
		logs.forEach((logLine -> httpAccessLogStatsComponent.compute(httpAccessLogStats, logLine)));

		//once every single log line has been computed, aggregate all of them -> this allows to get the MAX (top) statistics.
		httpAccessLogStatsComponent.aggregate(httpAccessLogStats);
		return httpAccessLogStats;
	}

	/**
	 * Save last sequence ID used for retrieving stats.
	 * This allows to not loose data between Scheduler executions.
	 * lastSeqId has synchronized access.
	 */
	private void saveLastSeqId(long lastSeqId) {
		this.lastSeqId.set(lastSeqId);;
	}
}