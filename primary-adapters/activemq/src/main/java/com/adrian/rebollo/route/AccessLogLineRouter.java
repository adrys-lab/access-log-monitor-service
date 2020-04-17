package com.adrian.rebollo.route;

import static com.adrian.rebollo.helper.ActiveMqDestinationBuilder.queue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.adrian.rebollo.PrimaryEndpoint;
import com.adrian.rebollo.api.AccessLogStatsService;
import com.adrian.rebollo.helper.EnhancedRouteBuilder;
import com.adrian.rebollo.model.AccessLogLine;
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

	@Override
	public void configure() {
		errorHandler(deadLetterChannel(queue(endpoint.getInternalLogLineQueueDead()).build()));

		/**
		 * Use the threadPoolSize and maxThreadPoolSize to allow parallel messages handling.
		 * This helps to speed-up log messages ingestion.
		 *
		 * example log stack (note the thread name):
		 *
		 * 2020-04-17 12:09:43,532 INFO  [Camel (camel-1) thread #31 - JmsConsumer[internal-log-line]] com.adrian.rebollo.service
		 * .AccessLogStatsServiceImpl: Saving log line accessLogLine=AccessLogLine
		 * 2020-04-17 12:09:43,541 INFO  [Camel (camel-1) thread #33 - JmsConsumer[internal-log-line]] com.adrian.rebollo.service
		 * .AccessLogStatsServiceImpl: Saving log line accessLogLine=AccessLogLine
		 * 2020-04-17 12:09:43,547 INFO  [Camel (camel-1) thread #22 - JmsConsumer[internal-log-line]] com.adrian.rebollo.service
		 * .AccessLogStatsServiceImpl: Saving log line accessLogLine=AccessLogLine
		 * 2020-04-17 12:09:43,551 INFO  [Camel (camel-1) thread #35 - JmsConsumer[internal-log-line]] com.adrian.rebollo.service
		 * .AccessLogStatsServiceImpl: Saving log line accessLogLine=AccessLogLine
		 * 2020-04-17 12:09:43,558 INFO  [Camel (camel-1) thread #5 - JmsConsumer[internal-log-line]] com.adrian.rebollo.service
		 * .AccessLogStatsServiceImpl: Saving log line accessLogLine=AccessLogLine
		 * 2020-04-17 12:09:43,564 INFO  [Camel (camel-1) thread #24 - JmsConsumer[internal-log-line]] com.adrian.rebollo.service
		 * .AccessLogStatsServiceImpl: Saving log line accessLogLine=AccessLogLine
		 */
		from(queue(endpoint.getInternalLogLineQueue())
				.setConcurrentConsumers(threadPoolSize)
				.setMaxConcurrentConsumers(maxThreadPoolSize)
				.setTransacted(true).build())
				.process((exchange) -> accessLogStatsService.handle(objectMapper.readValue(exchange.getIn().getBody(String.class), AccessLogLine.class)));
	}
}
