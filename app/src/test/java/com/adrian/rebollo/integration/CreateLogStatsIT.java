package com.adrian.rebollo.integration;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import com.adrian.rebollo.AccessLogMonitorApp;
import com.adrian.rebollo.config.ContainerEnvironment;
import com.adrian.rebollo.config.HttpAccessLogStatsJmsListener;
import com.adrian.rebollo.config.TestConfiguration;
import com.adrian.rebollo.dao.HttpAccessLogStatsDao;
import com.adrian.rebollo.model.HttpAccessLogStats;

@ActiveProfiles("dev")
@SpringBootTest(classes = { AccessLogMonitorApp.class } )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = TestConfiguration.class, initializers = { ContainerEnvironment.Initializer.class })
public class CreateLogStatsIT {

	private static final int TEST_TIMEOUT_IN_SECONDS = 30;

	@Autowired
	private HttpAccessLogStatsDao dao;

	@Autowired
	private HttpAccessLogStatsJmsListener httpAccessLogStatsJmsListener;

	@Value("${service.reader.file-name}")
	private String fileName;

	private File accessLogFile;

	@BeforeEach
	public void init() {
		accessLogFile = Paths.get(fileName).toFile();
	}

	@Test
	void testAddNewLogLineAndCheckStats() throws Exception {

		//////////////////GIVEN 3 LOG LINES TO BE READ /////////////////////////

		final String logLine = "127.123.22.54 user adrian [09/May/2018:16:00:42 +0000] \"POST /buy/user HTTP/1.0\" 503 100";
		final String secondLogLine = "11.32.54.66 user2 antonio [25/Dec/2015:16:00:42 +0000] \"PATCH /user/user HTTP/1.0\" 200 200";
		final String thirdLogLine = "164.33.2.63 user3 mia [17/Mar/2019:16:00:42 +0000] \"GET /employee/user HTTP/1.0\" 200 100";

		writeNewLine(logLine + "\n");
		writeNewLine(secondLogLine + "\n");
		writeNewLine(thirdLogLine + "\n");

		//////////////////WHEN PARSED AND READ BY THE CORE APPLICATION READ /////////////////////////

		final LocalDateTime now = LocalDateTime.now().plus(2, ChronoUnit.MINUTES);
		final LocalDateTime start = LocalDateTime.now().minus(2, ChronoUnit.MINUTES);

		//////////////////THEN IN THE DDBB MUST EXIST THE PERSISTED STATS /////////////////////////

		final List<HttpAccessLogStats> insertedStats = await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> dao.findByDateBetween(start, now, 1), Optional::isPresent).orElseThrow(IllegalStateException::new);

		final HttpAccessLogStats expected = new HttpAccessLogStats()
				.setRequests(new AtomicLong(3L))
				.setTotalContent(new AtomicLong(400))
				.setValidRequests(new AtomicLong(2))
				.setInvalidRequests(new AtomicLong(1));

		//////////////////AND ITS DATA MUST BE ACCORDING THE ADDED LOG LINES /////////////////////////

		final HttpAccessLogStats enqueuedMessage = await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> httpAccessLogStatsJmsListener.getHttpAccessLogStats(), Optional::isPresent).orElseThrow(IllegalStateException::new);

		//////////////////AND THE MESSAGE ENQUEUED HAVE CORRECT DATA TOO /////////////////////////

		assertJmsMessage(expected, enqueuedMessage);
	}

	private void assertJmsMessage(HttpAccessLogStats expected, HttpAccessLogStats enqueuedMessage) {
		Assert.assertNotNull(enqueuedMessage);

		Assert.assertEquals(expected.getRequests().get(), enqueuedMessage.getRequests().get());
		Assert.assertEquals(expected.getTotalContent().get(), enqueuedMessage.getTotalContent().get());
		Assert.assertEquals(expected.getValidRequests().get(), enqueuedMessage.getValidRequests().get());
		Assert.assertEquals(expected.getInvalidRequests().get(), enqueuedMessage.getInvalidRequests().get());
		Assert.assertTrue(enqueuedMessage.getTopInvalidVisitedRequestsSections().containsKey("/buy") && enqueuedMessage.getTopInvalidVisitedRequestsSections().get("/buy").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopValidVisitedRequestsSections().containsKey("/user") && enqueuedMessage.getTopValidVisitedRequestsSections().get("/user").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopValidVisitedRequestsSections().containsKey("/employee") && enqueuedMessage.getTopValidVisitedRequestsSections().get("/employee").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByUser().containsKey("antonio") && enqueuedMessage.getTopVisitsByUser().get("antonio").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByUser().containsKey("mia") && enqueuedMessage.getTopVisitsByUser().get("mia").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByUser().containsKey("adrian") && enqueuedMessage.getTopVisitsByUser().get("adrian").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByMethod().containsKey("POST") && enqueuedMessage.getTopVisitsByMethod().get("POST").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByMethod().containsKey("GET") && enqueuedMessage.getTopVisitsByMethod().get("GET").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByMethod().containsKey("PATCH") && enqueuedMessage.getTopVisitsByMethod().get("PATCH").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByHost().containsKey("11.32.54.66") && enqueuedMessage.getTopVisitsByHost().get("11.32.54.66").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByHost().containsKey("127.123.22.54") && enqueuedMessage.getTopVisitsByHost().get("127.123.22.54").get() == 1L);
		Assert.assertTrue(enqueuedMessage.getTopVisitsByHost().containsKey("164.33.2.63") && enqueuedMessage.getTopVisitsByHost().get("164.33.2.63").get() == 1L);
	}

	private void writeNewLine(final String logLine) throws IOException {
		FileUtils.writeStringToFile(accessLogFile, logLine, "UTF-8", true);
	}
}
