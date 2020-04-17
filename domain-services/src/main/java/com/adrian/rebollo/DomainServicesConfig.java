package com.adrian.rebollo;

import java.nio.file.Paths;

import org.apache.commons.io.input.Tailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.adrian.rebollo.reader.CustomTailerListener;

@Configuration
@EnableAsync
@EnableScheduling
public class DomainServicesConfig {

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
