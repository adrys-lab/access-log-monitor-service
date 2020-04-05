package com.adrian.rebollo.integration;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.adrian.rebollo.AccessLogMonitorApp;
import com.adrian.rebollo.config.ContainerEnvironment;
import com.adrian.rebollo.config.TestConfiguration;
import com.adrian.rebollo.entity.LogStats;
import com.adrian.rebollo.model.AlertType;
import com.adrian.rebollo.stats.HttpAccessLogStatsRepository;

@ActiveProfiles("dev")
@SpringBootTest(classes = { AccessLogMonitorApp.class } )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = TestConfiguration.class, initializers = { ContainerEnvironment.Initializer.class })
public class CreateLogAlertIT {

	private static final int TEST_TIMEOUT_IN_SECONDS = 60;

	@Autowired
	private HttpAccessLogStatsRepository statsRepository;
	@Autowired
	private StateMachine<AlertType, AlertType> alertStateMachine;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private SimpleJdbcInsert simpleJdbcInsert;

	@BeforeEach
	public void init() {
		alertStateMachine.sendEvent(AlertType.NO_ALERT);
	}

	////////////////// THIS IS AN SLOW TEST - MUST TEST FOR ~ 1 MINUTE DUE TO ALERTS STATE TRANSITIONS /////////////////////////
	////////////////// NOTICE THIS TEST RUNS WITH AN ALERT WINDOW OF 30 SECONDS AND THRESHOLD OF 5 REQUESTS/SEC TO NOT INCREASE TEST DURATION /////////////////////////
	@Test
	void testAddAlertTransitions() throws Exception {

		//////////////////GIVEN 1 stats with 3000 LOG LINES /////////////////////////
		LogStats logStats = new LogStats()
				.setRequests(3000L)
				.setValidRequests(3000L)
				.setInvalidRequests(0L)
				.setTotalContent(100000L);
		logStats.setSeqId(99999L);
		logStats.setInsertTime(LocalDateTime.now());
		this.persist(logStats);

		//////////////////WHEN INSERTED STATS WITH 3000 REQUESTS DURING LAST 30SEC /////////////////////////
		//////////////////THEN ALERT STATE MACHINE SHOULD BE HIGH TRAFFIC DUE TO 3000 ACCESSES IN LESS THAN 30 SECONDS  /////////////////////////
		await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> alertStateMachine.getState().getId() == AlertType.HIGH_TRAFFIC);

		//////////////////THEN ALERT STATE MACHINE SHOULD BE RECOVER DUE TO REDUCED ACCESES DURING LAST 30 SECONDS  /////////////////////////
		await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> alertStateMachine.getState().getId() == AlertType.RECOVER);

		//////////////////THEN ALERT STATE MACHINE SHOULD BE NO_ALERT DUE TO REDUCED ACCESES DURING LAST 30 SECONDS  /////////////////////////
		await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> alertStateMachine.getState().getId() == AlertType.NO_ALERT);

		delete(logStats);
	}

	public void persist(LogStats logStats) {
		simpleJdbcInsert.withTableName("stats");

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("seq_id", logStats.getSeqId());
		parameters.put("insert_time", LocalDateTime.now().minus(10, ChronoUnit.SECONDS));
		parameters.put("requests", logStats.getRequests());
		parameters.put("valid_requests", logStats.getValidRequests());
		parameters.put("invalid_requests", logStats.getInvalidRequests());
		parameters.put("total_content", logStats.getTotalContent());

		simpleJdbcInsert.execute(parameters);
	}

	public void delete(LogStats logStats) {
		jdbcTemplate.update("DELETE FROM stats WHERE seq_id = ?", logStats.getSeqId());
	}
}
