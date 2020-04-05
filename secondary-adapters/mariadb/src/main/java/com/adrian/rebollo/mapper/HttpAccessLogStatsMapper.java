package com.adrian.rebollo.mapper;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.adrian.rebollo.entity.LogStats;
import com.adrian.rebollo.model.HttpAccessLogStats;

@Component
public class HttpAccessLogStatsMapper implements BiMapper<LogStats, HttpAccessLogStats> {

	@Override
	public HttpAccessLogStats toDomain(final LogStats logStats) {
		return new HttpAccessLogStats()
				.setRequests(new AtomicLong((logStats.getRequests())))
				.setValidRequests(new AtomicLong((logStats.getValidRequests())))
				.setInvalidRequests(new AtomicLong(logStats.getInvalidRequests()))
				.setTotalContent(new AtomicLong(logStats.getTotalContent()));
	}

	@Override
	public LogStats toEntity(final HttpAccessLogStats httpAccessLogStats) {
		LogStats logStats = new LogStats()
				.setRequests(httpAccessLogStats.getRequests().get())
				.setValidRequests(httpAccessLogStats.getValidRequests().get())
				.setInvalidRequests(httpAccessLogStats.getInvalidRequests().get())
				.setTotalContent(httpAccessLogStats.getTotalContent().get());
		logStats.setInsertTime(LocalDateTime.now());
		return logStats;
	}
}
