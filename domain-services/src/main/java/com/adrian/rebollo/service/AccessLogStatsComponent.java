package com.adrian.rebollo.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.adrian.rebollo.exception.LogLineStatsParsingException;
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.AccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogStatsComponent {

	/**
	 * ranges of successful HTTP Codes.
	 */
	private final Range<Integer> successRange = Range.between(100, 299);

	/**
	 * ranges of failed HTTP Codes.
	 * 300-399 is ambigous as its not failed but not considered OK responses status.
	 */
	private final Range<Integer> failedRange = Range.between(300, 599);

	/**
	 * this method aggregates all the given LogLines into an AccessLogStats object.
	 * Computes all loglines independently, and afterwards aggregates all them together to obtain the top stats.
	 * @param logs to aggregate
	 * @return AccessLogStats
	 */
	AccessLogStats aggregateLogs(final List<AccessLogLine> logs) {

		final AccessLogStats accessLogStats = new AccessLogStats();

		//get min and max date in the same loop iteration (not double loop to get both)
		final LongSummaryStatistics instantLogStatistics = getInstantLogStatistics(logs);
		accessLogStats.setStart(fromEpoch(instantLogStatistics.getMin()));
		accessLogStats.setEnd(fromEpoch(instantLogStatistics.getMax()));

		LOG.info("Started aggregating {} log lines data.", logs.size());

		//compute every single Log Line independently.
		logs.forEach((logLine -> compute(accessLogStats, logLine)));

		//once every single log line has been computed, aggregate all of them -> this allows to get the MAX (top) statistics.
		aggregate(accessLogStats);
		return accessLogStats;
	}

	private LocalDateTime fromEpoch(long epoch) {
		return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	private LongSummaryStatistics getInstantLogStatistics(List<AccessLogLine> logs) {
		return logs.stream()
				.map(AccessLogLine::getInsertTime)
				.map((date) -> date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
				.mapToLong(Long::new)
				.summaryStatistics();
	}

	private void compute(AccessLogStats accessLogStats, AccessLogLine logLine) {

		Objects.requireNonNull(logLine, "logLine has ben called to compute with null value.");
		Objects.requireNonNull(accessLogStats, "httpAccessLogStats has ben called to compute with null value.");

		accessLogStats.getRequests().getAndIncrement();
		accessLogStats.getTotalContent().getAndAccumulate(logLine.getContentSize(), Long::sum);

		incrementOrPut(accessLogStats.getTopVisitsByHost(), logLine.getHost());
		incrementOrPut(accessLogStats.getTopVisitsByMethod(), logLine.getHttpMethod().name());
		incrementOrPut(accessLogStats.getTopVisitsByUser(), logLine.getUser());

		final String section = getSection(logLine.getResource());

		incrementOrPut(accessLogStats.getTopVisitsSection(), section);

		if(successRange.contains(logLine.getReturnedStatus())) {
			accessLogStats.getValidRequests().getAndIncrement();
			incrementOrPut(accessLogStats.getTopValidVisitedRequestsSections(), section);
		} else if(failedRange.contains(logLine.getReturnedStatus())) {
			accessLogStats.getInvalidRequests().getAndIncrement();
			incrementOrPut(accessLogStats.getTopInvalidVisitedRequestsSections(), section);
		}
	}

	private void aggregate(final AccessLogStats accessLogStats) {

		Objects.requireNonNull(accessLogStats, "httpAccessLogStats has ben called to aggregate with null value.");

		//This initializes all the Top statistics with the previously computed stats.
		//afterwards, it calls `topifyMap` which orders from max to min, and limit the results to 10, for each top stat.

		final Map<String, AtomicLong> hosts = new HashMap<>(accessLogStats.getTopVisitsByHost());
		accessLogStats.getTopVisitsByHost().clear();
		accessLogStats.getTopVisitsByHost().putAll(topifyMap(hosts));

		final Map<String, AtomicLong> method = new HashMap<>(accessLogStats.getTopVisitsByMethod());
		accessLogStats.getTopVisitsByMethod().clear();
		accessLogStats.getTopVisitsByMethod().putAll(topifyMap(method));

		final Map<String, AtomicLong> users = new HashMap<>(accessLogStats.getTopVisitsByUser());
		accessLogStats.getTopVisitsByUser().clear();
		accessLogStats.getTopVisitsByUser().putAll(topifyMap(users));

		final Map<String, AtomicLong> allSections = new HashMap<>(accessLogStats.getTopVisitsSection());
		accessLogStats.getTopVisitsSection().clear();
		accessLogStats.getTopVisitsSection().putAll(topifyMap(allSections));

		final Map<String, AtomicLong> validSections = new HashMap<>(accessLogStats.getTopValidVisitedRequestsSections());
		accessLogStats.getTopValidVisitedRequestsSections().clear();
		accessLogStats.getTopValidVisitedRequestsSections().putAll(topifyMap(validSections));

		final Map<String, AtomicLong> invalidSections = new HashMap<>(accessLogStats.getTopInvalidVisitedRequestsSections());
		accessLogStats.getTopInvalidVisitedRequestsSections().clear();
		accessLogStats.getTopInvalidVisitedRequestsSections().putAll(topifyMap(invalidSections));
	}

	/**
	 * make a map top by limiting to 10 items, ordered and keeping that order with LinkedHashMap.
	 */
	private Map<String, AtomicLong> topifyMap(Map<String, AtomicLong> map) {
		return map.entrySet()
		.stream()
		.sorted((c1, c2) -> Long.valueOf(c2.getValue().get()).compareTo(c1.getValue().get()))
		.limit(10)
		.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(e1, e2) -> e1, LinkedHashMap::new));
	}

	/**
	 * puts an initial value in the map with 0 if not exists.
	 * afterwards, accumulate the counter.
	 */
	private void incrementOrPut(Map<String, AtomicLong> map, String key) {
		map.putIfAbsent(key, new AtomicLong(0));
		map.get(key).getAndIncrement();
	}

	/**
	 * extract the section from the resource.
	 */
	private String getSection(String resource) {

		if( StringUtils.countMatches(resource, "/") == 1) {
			return resource;
		} else if( StringUtils.countMatches(resource, "/") > 1) {
			return resource.substring(0, resource.indexOf("/", 1));
		}

		throw new LogLineStatsParsingException(String.format("The given resource is not well formatted resource=%s", resource));
	}
}