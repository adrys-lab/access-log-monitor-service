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

import com.adrian.rebollo.api.AccessLogStatsService;
import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.AccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogStatsServiceImpl implements AccessLogStatsService {

	//Concurrent thread-safe queue with fastest peek, offer and poll operations (complexity O(1) for all 3 operations.)
	private final Queue<AccessLogLine> logLines = new ConcurrentLinkedQueue<>();
	private final InternalDispatcher internalDispatcher;
	private final AccessLogStatsComponent accessLogStatsComponent;

	@Value("${service.schedulers.stats.delay}")
	private int schedulerDelay;

	@Override
	public void handle(AccessLogLine accessLogLine) {
		LOG.info("Saving log line accessLogLine={}.", accessLogLine);

		//keep the access log lines in memory inside the logLines ConcurrentLinkedQueue
		logLines.offer(accessLogLine);
	}

	/**
	 * This scheduler triggers Log Stats aggregations.
	 * Triggered each Xsec (default 10sec).
	 * This aggregates log lines, compute them, and dispatch through internal Message Broker
	 */
	@ConditionalOnProperty( "service.schedulers.stats.enabled" )
	@Scheduled(fixedDelayString = "${service.schedulers.stats.delay}", initialDelayString = "${service.schedulers.stats.delay}")
	void aggregate() {

		final LocalDateTime end = LocalDateTime.now();
		final LocalDateTime start = end.minus(schedulerDelay, ChronoUnit.MILLIS);

		LOG.info("Triggered scheduler to aggregate logs statistics start={}, end={}.", start, end);

		final List<AccessLogLine> logs = getLogCandidates(end);

		//aggregate all the log line candidates
		final AccessLogStats accessLogStats = logs.isEmpty() ? AccessLogStats.with(start, end) : accessLogStatsComponent.aggregateLogs(logs);

		LOG.info("Finished aggregation httpAccessLogStats={}.", accessLogStats);

		//always dispatch the stats.
		internalDispatcher.dispatch(accessLogStats);
	}

	/*
	 * return the log lines candidates to be aggregated
	 */
	private List<AccessLogLine> getLogCandidates(LocalDateTime end) {

		final List<AccessLogLine> logs = new ArrayList<>();

		AccessLogLine log = logLines.peek();

		//first we peek a queue element to be checked, if satisfies the condition, we poll it.
		//this avoids to poll any log line from the queue which maybe does not satisfy the time condition.
		//LogLine candidates must satisfy that they have been inserted/parsed during the last 10 seconds.
		while (log != null && log.getInsertTime().isBefore(end)) {
			log = logLines.poll();
			logs.add(log);
			log = logLines.peek();
		}

		return logs;
	}
}