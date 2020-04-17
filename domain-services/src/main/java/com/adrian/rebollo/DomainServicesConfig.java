package com.adrian.rebollo;

import java.nio.file.Paths;
import java.util.concurrent.Executor;

import org.apache.commons.io.input.Tailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.adrian.rebollo.reader.CustomTailerListener;

@Configuration
@EnableAsync
@EnableScheduling
public class DomainServicesConfig {

	@Value("${service.thread-pool-size}")
	private int threadPoolSize;
	@Value("${service.max-thread-pool-size}")
	private int maxThreadPoolSize;

	/**
	 * Configure a ThreadPoolTaskExecutor to consume the input access.log file, process the line and dispatch it.
	 */
	@Primary
	@Bean(name = "httpLogReaderThreadPool")
	public Executor httpLogMonitorThreadPool() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(threadPoolSize);
		executor.setMaxPoolSize(maxThreadPoolSize);
		executor.setQueueCapacity(5000);
		executor.setThreadNamePrefix("HttpLogReaderThreadPool-");
		executor.initialize();
		return executor;
	}

	/**
	 * Provide a Configured Tailer instance.
	 */
	@Bean
	public Tailer configuredTailer(@Value("${service.reader.file-name}") String fileName,
			@Value("${service.reader.delay}") int delay,
			final CustomTailerListener customTailerListener) {
		return new Tailer(Paths.get(fileName).toFile(), customTailerListener, delay, true);
	}
}
