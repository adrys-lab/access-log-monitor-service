package com.adrian.rebollo.json;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.adrian.rebollo.log.LogExternalDispatcherImpl;
import com.adrian.rebollo.model.AlertType;
import com.adrian.rebollo.model.HttpAccessLogAlert;
import com.adrian.rebollo.model.HttpAccessLogStats;

public class LogExternalDispatcherImplTest {

	private LogExternalDispatcherImpl logService = new LogExternalDispatcherImpl();

	@Test
	public void handleStats() {
		final HttpAccessLogStats stats = new HttpAccessLogStats()
				.setRequests(new AtomicLong(3L))
				.setTotalContent(new AtomicLong(400))
				.setValidRequests(new AtomicLong(2))
				.setInvalidRequests(new AtomicLong(1));
		logService.dispatch(stats);
	}

	@Test
	public void handleAlerts() {
		final HttpAccessLogAlert alert = HttpAccessLogAlert.builder()
				.alertTime(LocalDateTime.now())
				.start(LocalDateTime.now())
				.end(LocalDateTime.now())
				.requests(400)
				.requestsSecond(24.76)
				.type(AlertType.HIGH_TRAFFIC)
				.build();
		logService.dispatch(alert);
	}
}
