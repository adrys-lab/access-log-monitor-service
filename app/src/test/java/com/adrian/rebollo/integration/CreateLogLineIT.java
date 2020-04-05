package com.adrian.rebollo.integration;

import static org.awaitility.Awaitility.await;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import com.adrian.rebollo.AccessLogMonitorApp;
import com.adrian.rebollo.config.ContainerEnvironment;
import com.adrian.rebollo.config.TestConfiguration;
import com.adrian.rebollo.dao.HttpAccessLogLineDao;

@ActiveProfiles("dev")
@SpringBootTest(classes = { AccessLogMonitorApp.class } )
@ExtendWith(ContainerEnvironment.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = TestConfiguration.class, initializers = { ContainerEnvironment.Initializer.class })
public class CreateLogLineIT {

	private static final int TEST_TIMEOUT_IN_SECONDS = 40;

	@Autowired
	private HttpAccessLogLineDao dao;

	@Value("${service.reader.file-name}")
	private String fileName;

	private File accessLogFile;

	@BeforeEach
	public void init() {
		accessLogFile = Paths.get(fileName).toFile();
	}

	@Test
	void testAddNewLine() throws Exception {

		//////////////////GIVEN 3 LOG LINES TO BE READ /////////////////////////
		final String logLine = "127.0.0.1 loggeduser mary [09/May/2018:16:00:42 +0000] \"POST /api/user HTTP/1.0\" 503 12";

		writeNewLine(logLine + "\n");

		//////////////////WHEN PARSED AND READ BY THE CORE APPLICATION READ /////////////////////////

		//////////////////THEN IN THE DDBB MUST EXIST THE PERSISTED LOG LINE /////////////////////////

		await().atMost(Duration.ofSeconds(TEST_TIMEOUT_IN_SECONDS))
				.until(() -> dao.findBySeqIdGreater(0, 1), Optional::isPresent).orElseThrow(IllegalStateException::new);
	}

	private void writeNewLine(final String logLine) throws IOException {
		FileUtils.writeStringToFile(accessLogFile, logLine, "UTF-8", true);
	}
}
