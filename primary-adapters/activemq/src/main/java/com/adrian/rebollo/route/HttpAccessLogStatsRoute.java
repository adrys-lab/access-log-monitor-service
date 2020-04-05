package com.adrian.rebollo.route;

import static com.adrian.rebollo.helper.ActiveMqDestinationBuilder.queue;

import org.springframework.stereotype.Component;

import com.adrian.rebollo.PrimaryEndpoint;
import com.adrian.rebollo.api.ExternalDispatcherObserver;
import com.adrian.rebollo.helper.EnhancedRouteBuilder;
import com.adrian.rebollo.model.HttpAccessLogStats;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Route for HttpAccessLogStats which are handled by the LogService
 */
@Component
@RequiredArgsConstructor
public class HttpAccessLogStatsRoute extends EnhancedRouteBuilder {

	private final PrimaryEndpoint endpoint;
	private final ObjectMapper objectMapper;
	private final ExternalDispatcherObserver externalDispatcherObserver;

	@Override
	public void configure() {
		errorHandler(deadLetterChannel(queue(endpoint.getInternalLogStatsQueueDead()).build()));

		from(queue(endpoint.getInternalLogStatsQueue()).setConcurrentConsumers(1).setTransacted(true).build())
				.process((exchange) -> {
					HttpAccessLogStats payload = objectMapper.readValue(exchange.getIn().getBody(String.class), HttpAccessLogStats.class);
					externalDispatcherObserver.notify(payload);
				});
	}
}
