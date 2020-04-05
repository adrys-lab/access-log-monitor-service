package com.adrian.rebollo.helper;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.spring.SpringRouteBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class EnhancedRouteBuilder extends SpringRouteBuilder {

	@Override
	public final DeadLetterChannelBuilder deadLetterChannel(String endpoint) {
		DefaultErrorHandlerBuilder builder = super.deadLetterChannel(endpoint)
				.useOriginalMessage()
				.log(LOG)
				.loggingLevel(LoggingLevel.ERROR)
				.logHandled(true)
				.logExhausted(true);
		return (DeadLetterChannelBuilder) builder;
	}

}
