package com.adrian.rebollo.route;

import static com.adrian.rebollo.helper.ActiveMqDestinationBuilder.queue;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.adrian.rebollo.PrimaryEndpoint;
import com.adrian.rebollo.api.AccessLogStatsService;
import com.adrian.rebollo.helper.EnhancedRouteBuilder;
import com.adrian.rebollo.model.AccessLogLine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Route for HttpAccessLogLine which are handled by the HttpAccessLogAlertService
 */
@Component
@RequiredArgsConstructor
public class AccessLogLineRouter extends EnhancedRouteBuilder {

	@Value("${service.thread-pool-size}")
	private int threadPoolSize;
	@Value("${service.max-thread-pool-size}")
	private int maxThreadPoolSize;

	private final PrimaryEndpoint endpoint;
	private final ObjectMapper objectMapper;
	private final AccessLogStatsService accessLogStatsService;
	private final Executor logConsumerThreadPool;

	@Override
	public void configure() {
		errorHandler(deadLetterChannel(queue(endpoint.getInternalLogLineQueueDead()).build()));

		from(queue(endpoint.getInternalLogLineQueue())
				.setConcurrentConsumers(threadPoolSize)
				.setMaxConcurrentConsumers(maxThreadPoolSize)
				.setTransacted(true).build())
				.process((exchange) ->
						/**
						 * Use the configured ThreadPoolTaskExecutor in {@link com.adrian.rebollo.AmqConfig} for parallel message handling for handling Log Lines.
						 * This helps to speed-up log messages ingestion.
						 */
						logConsumerThreadPool.execute(() -> {
							try {
								accessLogStatsService.handle(objectMapper.readValue(exchange.getIn().getBody(String.class), AccessLogLine.class));
							} catch (JsonProcessingException e) {
								e.printStackTrace();
							}
						})
				);
	}
}