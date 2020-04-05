package com.adrian.rebollo.config;

import java.util.Optional;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.stereotype.Component;

import com.adrian.rebollo.model.HttpAccessLogStats;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration class to bind the internal Stats Queue to a dedicated Test Listener for test purposes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpAccessLogStatsJmsListener implements MessageListener {

	private Optional<HttpAccessLogStats> httpAccessLogStats = Optional.empty();

	private final ObjectMapper objectMapper;

	@Override
	public void onMessage(Message message) {
		if (message instanceof TextMessage) {
			try {
				String text = ((TextMessage) message).getText();
				httpAccessLogStats = Optional.of(objectMapper.readValue(text, HttpAccessLogStats.class));
			} catch (Exception e) {
				LOG.error("Exception receiving message={}", message);
			}
		}
	}

	public Optional<HttpAccessLogStats> getHttpAccessLogStats() {
		return httpAccessLogStats;
	}
}