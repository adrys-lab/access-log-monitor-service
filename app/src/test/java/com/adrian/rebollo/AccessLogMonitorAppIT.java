package com.adrian.rebollo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.adrian.rebollo.config.ContainerEnvironment;
import com.adrian.rebollo.config.TestConfiguration;

@ActiveProfiles("dev")
@SpringBootTest(classes = { AccessLogMonitorApp.class } )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = TestConfiguration.class, initializers = { ContainerEnvironment.Initializer.class })
public class AccessLogMonitorAppIT {

	@Test
	public void contextLoads() {
	}
}