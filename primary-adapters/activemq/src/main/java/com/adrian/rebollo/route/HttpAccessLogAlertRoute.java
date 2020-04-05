package com.adrian.rebollo.route;

import static com.adrian.rebollo.helper.ActiveMqDestinationBuilder.queue;

import org.springframework.stereotype.Component;

import com.adrian.rebollo.PrimaryEndpoint;
import com.adrian.rebollo.api.ExternalDispatcherObserver;
import com.adrian.rebollo.helper.EnhancedRouteBuilder;
import com.adrian.rebollo.model.HttpAccessLogAlert;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Route for HttpAccessLogAlert which are handled by the LogService
 */
@Component
@RequiredArgsConstructor
public class HttpAccessLogAlertRoute extends EnhancedRouteBuilder {

	private final PrimaryEndpoint endpoint;
	private final ObjectMapper objectMapper;
	private final ExternalDispatcherObserver externalDispatcherObserver;

	@Override
	public void configure() {
		errorHandler(deadLetterChannel(queue(endpoint.getInternalLogAlertQueueDead()).build()));

		from(queue(endpoint.getInternalLogAlertQueue()).setConcurrentConsumers(1).setTransacted(true).build())
				.process((exchange) -> {
					HttpAccessLogAlert payload = objectMapper.readValue(exchange.getIn().getBody(String.class), HttpAccessLogAlert.class);
					externalDispatcherObserver.notify(payload);
				});
	}
}
