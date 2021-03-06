package com.adrian.rebollo.route;

import static com.adrian.rebollo.helper.ActiveMqDestinationBuilder.queue;

import org.springframework.stereotype.Component;

import com.adrian.rebollo.PrimaryEndpoint;
import com.adrian.rebollo.api.ExternalDispatcherObserver;
import com.adrian.rebollo.helper.EnhancedRouteBuilder;
import com.adrian.rebollo.model.AccessLogAlert;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Route for HttpAccessLogAlert which are handled by the LogService
 */
@Component
@RequiredArgsConstructor
public class AccessLogAlertRouter extends EnhancedRouteBuilder {

	private final PrimaryEndpoint endpoint;
	private final ObjectMapper objectMapper;
	private final ExternalDispatcherObserver externalDispatcherObserver;

	@Override
	public void configure() {
		errorHandler(deadLetterChannel(queue(endpoint.getInternalLogAlertQueueDead()).build()));

		from(queue(endpoint.getInternalLogAlertQueue())
				.setConcurrentConsumers(1)
				.setTransacted(true)
				.build())
				.process((exchange) -> {
					AccessLogAlert payload = objectMapper.readValue(exchange.getIn().getBody(String.class), AccessLogAlert.class);
					externalDispatcherObserver.notify(payload);
				});
	}
}
