package com.adrian.rebollo.parser;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Assert;
import org.junit.Test;

import com.adrian.rebollo.exception.HttpLogLineParsingException;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpMethod;

public class HttpAccessLogLineParserTest {

	private final HttpAccessLogLineParser httpAccessLogLineParser = new HttpAccessLogLineParser();
	private final String logLine = "127.0.0.1 loggeduser mary [09/May/2018:16:00:42 +0000] \"POST /api/user HTTP/1.0\" 503 12";
	private final String dasshedLine = "- - - [09/May/2018:16:00:42 +0000] \"POST /api/user HTTP/1.0\" 503 12";

	@Test
	public void testParse() {

		HttpAccessLogLine response = httpAccessLogLineParser.apply(logLine);

		Assert.assertEquals(HttpAccessLogLine.builder()
				.line(logLine)
				.host("127.0.0.1")
				.identifier("loggeduser")
				.user("mary")
				.dateTime(ZonedDateTime.of(LocalDateTime.of(2018, 5, 9, 16, 0, 42), ZoneId.of("+0000")).toLocalDateTime())
				.httpMethod(HttpMethod.POST)
				.resource("/api/user")
				.protocol("HTTP/1.0")
				.returnedStatus(503)
				.contentSize(12)
				.build(), response);
	}

	@Test
	public void testDashedLine() {

		HttpAccessLogLine response = httpAccessLogLineParser.apply(dasshedLine);

		Assert.assertEquals(HttpAccessLogLine.builder()
				.line(dasshedLine)
				.host("")
				.identifier("NO_IDENTIFIER")
				.user("NO_USER")
				.dateTime(ZonedDateTime.of(LocalDateTime.of(2018, 5, 9, 16, 0, 42), ZoneId.of("+0000")).toLocalDateTime())
				.httpMethod(HttpMethod.POST)
				.resource("/api/user")
				.protocol("HTTP/1.0")
				.returnedStatus(503)
				.contentSize(12)
				.build(), response);
	}

	@Test(expected = HttpLogLineParsingException.class)
	public void testExceptionForDate() {

		httpAccessLogLineParser.apply("127.0.0.1 loggeduser mary [50/May/2018:16:00:42 +0000] \"POST /api/user HTTP/1.0\" 503 12");
	}

	@Test(expected = HttpLogLineParsingException.class)
	public void testExceptionForMethod() {

		httpAccessLogLineParser.apply("127.0.0.1 loggeduser mary [50/May/2018:16:00:42 +0000] \"ANOTHERMETHOD /api/user HTTP/1.0\" 503 12");
	}
}
