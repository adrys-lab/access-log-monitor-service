package com.adrian.rebollo.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.adrian.rebollo.entity.LogLine;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpMethod;

@Component
public class HttpAccessLogLineMapper implements BiMapper<LogLine, HttpAccessLogLine> {

	@Override
	public HttpAccessLogLine toDomain(final LogLine logLine) {
		return HttpAccessLogLine.builder()
				.line(logLine.getLine())
				.insertTime(logLine.getInsertTime())
				.seqId(logLine.getSeqId())
				.host(logLine.getHost())
				.identifier(logLine.getIdentifier())
				.user(logLine.getUser())
				.dateTime(logLine.getDateTime())
				.httpMethod(HttpMethod.valueOf(logLine.getHttpMethod()))
				.resource(logLine.getResource())
				.protocol(logLine.getProtocol())
				.returnedStatus(logLine.getReturnedStatus())
				.contentSize(logLine.getContentSize())
				.build();
	}

	@Override
	public LogLine toEntity(final HttpAccessLogLine httpAccessLogLine) {
		final LogLine logLine = new LogLine()
				.setLine(httpAccessLogLine.getLine())
				.setHost(httpAccessLogLine.getHost())
				.setIdentifier(httpAccessLogLine.getIdentifier())
				.setUser(httpAccessLogLine.getUser())
				.setDateTime(httpAccessLogLine.getDateTime())
				.setHttpMethod(httpAccessLogLine.getHttpMethod().name())
				.setResource(httpAccessLogLine.getResource())
				.setProtocol(httpAccessLogLine.getProtocol())
				.setReturnedStatus(httpAccessLogLine.getReturnedStatus())
				.setContentSize(httpAccessLogLine.getContentSize());
		logLine.setInsertTime(LocalDateTime.now());
		return logLine;
	}
}
