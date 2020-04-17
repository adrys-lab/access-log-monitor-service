package com.adrian.rebollo.config;

import java.util.Optional;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import com.adrian.rebollo.model.AccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class to bind the internal Stats Queue to a dedicated Test Listener for test purposes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpAccessLogAlertJmsListener implements MessageListener {

	private Optional<AccessLogStats> httpAccessLogStats = Optional.empty();

	@Override
	public void onMessage(Message message) {
		if (message instanceof TextMessage) {
			try {
				String text = ((TextMessage) message).getText();
				httpAccessLogStats = Optional.of(new ObjectMapper().readValue(text, AccessLogStats.class));
			} catch (Exception e) {
				LOG.error("Exception receiving message={}", message);
			}
		}
	}

	public Optional<AccessLogStats> getHttpAccessLogStats() {
		return httpAccessLogStats;
	}
}