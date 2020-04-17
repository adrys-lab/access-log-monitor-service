package com.adrian.rebollo;

import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.AccessLogAlert;
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.AccessLogStats;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Internal dispatcher implementation to internally dispatch objects into AMQ destinations.
 */
@Slf4j
@Service
@EnableJms
@EnableScheduling
@RequiredArgsConstructor
public class PrimaryActiveMQ implements InternalDispatcher {

	private final JmsTemplate jmsTemplate;
	private final ObjectMapper objectMapper;
	private final PrimaryEndpoint endpoint;

	@Override
	public void dispatch(AccessLogLine accessLogLine) {
		send(endpoint.getInternalLogLineQueue(), accessLogLine);
	}

	@Override
	public void dispatch(AccessLogStats accessLogStats) {
		send(endpoint.getInternalLogStatsQueue(), accessLogStats);
	}

	@Override
	public void dispatch(AccessLogAlert accessLogAlert) {
		send(endpoint.getInternalLogAlertQueue(), accessLogAlert);
	}

	@SneakyThrows
	private void send(String destination, Object message) {
		var serializedMessage = (message instanceof String) ? (String) message : objectMapper.writeValueAsString(message);
		jmsTemplate.send(destination, session -> session.createTextMessage(serializedMessage));
	}
}
