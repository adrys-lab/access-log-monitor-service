package com.adrian.rebollo.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.api.HttpAccessLogStatsService;
import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpAccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpAccessLogStatsServiceImpl implements HttpAccessLogStatsService {

	//Concurrent thread-safe queue with fast insertion peek and deletion (complexity O(1) for all 3 operations.)
	private final Queue<HttpAccessLogLine> logLines = new ConcurrentLinkedQueue<>();
	private final InternalDispatcher internalDispatcher;
	private final HttpAccessLogStatsComponent httpAccessLogStatsComponent;

	@Value("${service.schedulers.stats.delay}")
	private int schedulerDelay;

	@Override
	public void handle(HttpAccessLogLine httpAccessLogLine) {
		logLines.offer(httpAccessLogLine);
	}

	/**
	 * This scheduler triggers Log Stats aggregations.
	 * Triggered each (default) 10 seconds.
	 * This aggregates log lines, compute them, and dispatch through internal Queue Message Broker
	 */
	@ConditionalOnProperty( "service.schedulers.stats.enabled" )
	@Scheduled(fixedDelayString = "${service.schedulers.stats.delay}", initialDelayString = "${service.schedulers.stats.delay}")
	private void aggregate() {

		final LocalDateTime end = LocalDateTime.now();
		final LocalDateTime start = end.minus(schedulerDelay, ChronoUnit.MILLIS);

		LOG.info("Triggered scheduler to aggregate logs statistics start={}, end={}.", start, end);

		//return the log lines candidates to be aggregated
		final List<HttpAccessLogLine> logs = getLogCandidates(end);

		//aggregate all the log line candidates
		final HttpAccessLogStats httpAccessLogStats = httpAccessLogStatsComponent.aggregateLogs(logs);

		LOG.info("Finished aggregation httpAccessLogStats={}.", httpAccessLogStats);

		//always dispatch the stats.
		internalDispatcher.dispatch(httpAccessLogStats);
	}

	private List<HttpAccessLogLine> getLogCandidates(LocalDateTime end) {
		final List<HttpAccessLogLine> logs = new ArrayList<>();

		HttpAccessLogLine log = logLines.peek();

		while (log != null && log.getInsertTime().isBefore(end)) {
			log = logLines.poll();
			logs.add(log);
			log = logLines.peek();
		}

		return logs;
	}
}