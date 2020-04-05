package com.adrian.rebollo.integration;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import com.adrian.rebollo.AccessLogMonitorApp;
import com.adrian.rebollo.config.ContainerEnvironment;
import com.adrian.rebollo.config.TestConfiguration;
import com.adrian.rebollo.model.AlertType;

@ActiveProfiles("dev")
@SpringBootTest(classes = { AccessLogMonitorApp.class } )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = TestConfiguration.class, initializers = { ContainerEnvironment.Initializer.class })
public class EndToEndIT {

	private static final int TEST_TIMEOUT_IN_SECONDS = 120;

	@Autowired
	private StateMachine<AlertType, AlertType> alertStateMachine;

	@Value("${service.reader.file-name}")
	private String fileName;

	private File accessLogFile;

	@BeforeEach
	public void init() {
		accessLogFile = Paths.get(fileName).toFile();
		alertStateMachine.sendEvent(AlertType.NO_ALERT);
	}

	////////////////// THIS IS AN SLOW TEST - MUST TEST FOR MORE THAN 2 MINUTES DUE TO ALERTS /////////////////////////
	////////////////// NOTICE THIS TEST RUNS WITH AN ALERT WINDOW OF 30 SECONDS AND THRESHOLD OF 5 REQUESTS/SEC TO NOT INCREASE TEST DURATION /////////////////////////

	@Test
	void endToEndTest() throws Exception {

		//////////////////GIVEN 3000 LOG LINES TO BE READ (each logline differs on content-length) /////////////////////////

		Thread.sleep(2000);

		final StringBuilder logLines = new StringBuilder();
		String logLine = "127.123.22.54 user adrian [09/May/2018:16:00:42 +0000] \"POST /buy/user HTTP/1.0\" 503 %s \n";
		int count = 0;

		for(; count < 3000; count++) {
			logLines.append(String.format(logLine, ++count));
		}
		writeNewLine(logLines.toString());

		//////////////////WHEN PARSED AND READ BY THE CORE APPLICATION READ /////////////////////////
		//////////////////THEN ALERT STATE MACHINE SHOULD BE HIGH TRAFFIC DUE TO 3000 ACCESSES IN LESS THAN 30 SECONDS  /////////////////////////
		await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> alertStateMachine.getState().getId() == AlertType.HIGH_TRAFFIC);

		//////////////////THEN ALERT STATE MACHINE SHOULD BE RECOVER DUE TO REDUCED ACCESES DURING LAST 30 SECONDS  /////////////////////////
		await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> alertStateMachine.getState().getId() == AlertType.RECOVER);

		//////////////////THEN ALERT STATE MACHINE SHOULD BE NO_ALERT DUE TO REDUCED ACCESES DURING LAST 30 SECONDS  /////////////////////////
		await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> alertStateMachine.getState().getId() == AlertType.NO_ALERT);
	}

	private void writeNewLine(final String logLine) throws IOException {
		FileUtils.writeStringToFile(accessLogFile, logLine, "UTF-8", true);
	}
}
