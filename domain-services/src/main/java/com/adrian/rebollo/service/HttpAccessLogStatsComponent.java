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

import com.adrian.rebollo.exception.HttpLogLineStatsParsingException;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpAccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpAccessLogStatsComponent {

	/**
	 * ranges of successful HTTP Codes.
	 */
	private final Range<Integer> successRange = Range.between(100, 299);

	/**
	 * ranges of failed HTTP Codes.
	 * 300-399 is ambigous as its not failed but not considered OK responses status.
	 */
	private final Range<Integer> failedRange = Range.between(300, 599);

	HttpAccessLogStats aggregateLogs(final List<HttpAccessLogLine> logs) {

		final HttpAccessLogStats httpAccessLogStats = new HttpAccessLogStats();

		//get min and max date in the same loop iteration (not double loop to get both)
		final LongSummaryStatistics instantLogStatistics = getInstantLogStatistics(logs);
		httpAccessLogStats.setStart(fromEpoch(instantLogStatistics.getMin()));
		httpAccessLogStats.setEnd(fromEpoch(instantLogStatistics.getMax()));

		LOG.info("Started aggregating {} log lines data.", logs.size());

		//compute every single Log Line independently.
		logs.forEach((logLine -> compute(httpAccessLogStats, logLine)));

		//once every single log line has been computed, aggregate all of them -> this allows to get the MAX (top) statistics.
		aggregate(httpAccessLogStats);
		return httpAccessLogStats;
	}

	private LocalDateTime fromEpoch(long epoch) {
		return Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	private LongSummaryStatistics getInstantLogStatistics(List<HttpAccessLogLine> logs) {
		return logs.stream()
				.map(HttpAccessLogLine::getInsertTime)
				.map((date) -> date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
				.mapToLong(Long::new)
				.summaryStatistics();
	}

	private void compute(HttpAccessLogStats httpAccessLogStats, HttpAccessLogLine logLine) {

		Objects.requireNonNull(logLine, "logLine has ben called to compute with null value.");
		Objects.requireNonNull(httpAccessLogStats, "httpAccessLogStats has ben called to compute with null value.");

		httpAccessLogStats.getRequests().getAndIncrement();
		httpAccessLogStats.getTotalContent().getAndAccumulate(logLine.getContentSize(), Long::sum);

		incrementOrPut(httpAccessLogStats.getTopVisitsByHost(), logLine.getHost());
		incrementOrPut(httpAccessLogStats.getTopVisitsByMethod(), logLine.getHttpMethod().name());
		incrementOrPut(httpAccessLogStats.getTopVisitsByUser(), logLine.getUser());

		final String section = getSection(logLine.getResource());

		incrementOrPut(httpAccessLogStats.getTopVisitsSection(), section);

		if(successRange.contains(logLine.getReturnedStatus())) {
			httpAccessLogStats.getValidRequests().getAndIncrement();
			incrementOrPut(httpAccessLogStats.getTopValidVisitedRequestsSections(), section);
		} else if(failedRange.contains(logLine.getReturnedStatus())) {
			httpAccessLogStats.getInvalidRequests().getAndIncrement();
			incrementOrPut(httpAccessLogStats.getTopInvalidVisitedRequestsSections(), section);
		}
	}

	private void aggregate(HttpAccessLogStats httpAccessLogStats) {

		Objects.requireNonNull(httpAccessLogStats, "httpAccessLogStats has ben called to aggregate with null value.");

		final Map<String, AtomicLong> hosts = new HashMap<>(httpAccessLogStats.getTopVisitsByHost());
		httpAccessLogStats.getTopVisitsByHost().clear();
		httpAccessLogStats.getTopVisitsByHost().putAll(topifyMap(hosts));

		final Map<String, AtomicLong> method = new HashMap<>(httpAccessLogStats.getTopVisitsByMethod());
		httpAccessLogStats.getTopVisitsByMethod().clear();
		httpAccessLogStats.getTopVisitsByMethod().putAll(topifyMap(method));

		final Map<String, AtomicLong> users = new HashMap<>(httpAccessLogStats.getTopVisitsByUser());
		httpAccessLogStats.getTopVisitsByUser().clear();
		httpAccessLogStats.getTopVisitsByUser().putAll(topifyMap(users));

		final Map<String, AtomicLong> allSections = new HashMap<>(httpAccessLogStats.getTopVisitsSection());
		httpAccessLogStats.getTopVisitsSection().clear();
		httpAccessLogStats.getTopVisitsSection().putAll(topifyMap(allSections));

		final Map<String, AtomicLong> validSections = new HashMap<>(httpAccessLogStats.getTopValidVisitedRequestsSections());
		httpAccessLogStats.getTopValidVisitedRequestsSections().clear();
		httpAccessLogStats.getTopValidVisitedRequestsSections().putAll(topifyMap(validSections));

		final Map<String, AtomicLong> invalidSections = new HashMap<>(httpAccessLogStats.getTopInvalidVisitedRequestsSections());
		httpAccessLogStats.getTopInvalidVisitedRequestsSections().clear();
		httpAccessLogStats.getTopInvalidVisitedRequestsSections().putAll(topifyMap(invalidSections));
	}

	/**
	 * make a map top by limiting 10 ordered and keeping that order with LinkedHashMap.
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

		throw new HttpLogLineStatsParsingException(String.format("The given resource is not well formatted resource=%s", resource));
	}
}