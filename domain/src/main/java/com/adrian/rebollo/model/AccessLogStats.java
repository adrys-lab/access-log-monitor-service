package com.adrian.rebollo.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO for all the Log Stats.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccessLogStats extends LogInfo {

	/**
	 * start of the Stats range time.
	 */
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
	private LocalDateTime start;

	/**
	 * end of the Stats range time.
	 */
	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
	private LocalDateTime end;

	/**
	 * Even thought there is not a multi-thread big requirement (Synchronized accesses to those fields), its easier to compute/increment with atomics.
	 * They could easily be migrated to basic longs, but i found it easier to compute/increment them.
	 */
	private AtomicLong requests = new AtomicLong();

	private AtomicLong validRequests = new AtomicLong();

	private AtomicLong invalidRequests = new AtomicLong();

	private AtomicLong totalContent = new AtomicLong();


	// ******** TOP STATISTICS ************//

	// LinkedHashMap keeps the inserted order
	/**
	 * keep the 10 requests methods that has been requested more times.
	 */
	private final Map<String, AtomicLong> topVisitsByMethod = new LinkedHashMap<>();

	/**
	 * keep the 10 sections that have been visited more times successfully.
	 */
	private final Map<String, AtomicLong> topValidVisitedRequestsSections = new LinkedHashMap<>();

	/**
	 * keep the 10 sections that have been visited more times failing.
	 */
	private final Map<String, AtomicLong> topInvalidVisitedRequestsSections = new LinkedHashMap<>();

	/**
	 * keep the 10 hosts that accessed more times.
	 */
	private final Map<String, AtomicLong> topVisitsByHost = new LinkedHashMap<>();

	/**
	 * keep the 10 users that accessed more times.
	 */
	private final Map<String, AtomicLong> topVisitsByUser = new LinkedHashMap<>();
	/**
	 * keep the 10 most visited sections.
	 */
	private final Map<String, AtomicLong> topVisitsSection = new LinkedHashMap<>();

	public AccessLogStats(LocalDateTime start, LocalDateTime end) {
		this.start = start;
		this.end = end;
	}

	public static AccessLogStats empty(LocalDateTime start, LocalDateTime end) {
		return new AccessLogStats().setStart(start).setEnd(end);
	}
}
