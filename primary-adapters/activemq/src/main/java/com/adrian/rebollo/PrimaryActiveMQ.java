package com.adrian.rebollo;

import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.HttpAccessLogAlert;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpAccessLogStats;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

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
	public void dispatch(HttpAccessLogLine httpAccessLogLine) {
		send(endpoint.getInternalLogLineQueue(), httpAccessLogLine);
	}

	@Override
	public void dispatch(HttpAccessLogStats httpAccessLogStats) {
		send(endpoint.getInternalLogStatsQueue(), httpAccessLogStats);
	}

	@Override
	public void dispatch(HttpAccessLogAlert httpAccessLogAlert) {
		send(endpoint.getInternalLogAlertQueue(), httpAccessLogAlert);
	}

	@SneakyThrows
	private void send(String destination, Object message) {
		var serializedMessage = (message instanceof String) ? (String) message : objectMapper.writeValueAsString(message);
		jmsTemplate.send(destination, session -> session.createTextMessage(serializedMessage));
	}
}
