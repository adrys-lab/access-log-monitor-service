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
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.AccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpAccessLogStatsServiceImpl implements HttpAccessLogStatsService {

	//Concurrent thread-safe queue with fast insertion peek and deletion (complexity O(1) for all 3 operations.)
	private final Queue<AccessLogLine> logLines = new ConcurrentLinkedQueue<>();
	private final InternalDispatcher internalDispatcher;
	private final HttpAccessLogStatsComponent httpAccessLogStatsComponent;

	@Value("${service.schedulers.stats.delay}")
	private int schedulerDelay;

	@Override
	public void handle(AccessLogLine accessLogLine) {
		logLines.offer(accessLogLine);
	}

	/**
	 * This scheduler triggers Log Stats aggregations.
	 * Triggered each (default) 10 seconds.
	 * This aggregates log lines, compute them, and dispatch through internal Queue Message Broker
	 */
	@ConditionalOnProperty( "service.schedulers.stats.enabled" )
	@Scheduled(fixedDelayString = "${service.schedulers.stats.delay}", initialDelayString = "${service.schedulers.stats.delay}")
	void aggregate() {

		final LocalDateTime end = LocalDateTime.now();
		final LocalDateTime start = end.minus(schedulerDelay, ChronoUnit.MILLIS);

		LOG.info("Triggered scheduler to aggregate logs statistics start={}, end={}.", start, end);

		//return the log lines candidates to be aggregated
		final List<AccessLogLine> logs = getLogCandidates(end);

		//aggregate all the log line candidates
		final AccessLogStats accessLogStats = logs.isEmpty() ? AccessLogStats.empty(start, end) : httpAccessLogStatsComponent.aggregateLogs(logs);

		LOG.info("Finished aggregation httpAccessLogStats={}.", accessLogStats);

		//always dispatch the stats.
		internalDispatcher.dispatch(accessLogStats);
	}

	private List<AccessLogLine> getLogCandidates(LocalDateTime end) {
		final List<AccessLogLine> logs = new ArrayList<>();

		AccessLogLine log = logLines.peek();

		while (log != null && log.getInsertTime().isBefore(end)) {
			log = logLines.poll();
			logs.add(log);
			log = logLines.peek();
		}

		return logs;
	}
}