package com.adrian.rebollo.json;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.adrian.rebollo.model.AlertType;
import com.adrian.rebollo.model.AccessLogAlert;
import com.adrian.rebollo.model.AccessLogStats;
import com.fasterxml.jackson.databind.json.JsonMapper;

public class JsonExternalDispatcherImplTest {

	private JsonDispatcherComponent logComponent = new JsonDispatcherComponent(new JsonMapper());
	private JsonExternalDispatcherImpl logService = new JsonExternalDispatcherImpl(logComponent);

	@Before
	public void init() {
		ReflectionTestUtils.setField(logComponent, "fileName", "/test.json");
		ReflectionTestUtils.setField(logService, "fileName", "/test.json");
	}

	@Test
	public void handleStats() {
		final AccessLogStats stats = new AccessLogStats()
				.setRequests(new AtomicLong(3L))
				.setTotalContent(new AtomicLong(400))
				.setValidRequests(new AtomicLong(2))
				.setInvalidRequests(new AtomicLong(1));
		logService.init();
		logService.dispatch(stats);
	}

	@Test
	public void handleAlerts() {
		final AccessLogAlert alert = AccessLogAlert.builder()
				.alertTime(LocalDateTime.now())
				.start(LocalDateTime.now())
				.end(LocalDateTime.now())
				.requests(400)
				.requestsSecond(24.76)
				.type(AlertType.HIGH_TRAFFIC)
				.build();
		logService.init();
		logService.dispatch(alert);
	}
}
