package com.adrian.rebollo.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Assert;
import org.junit.Test;

import com.adrian.rebollo.exception.HttpLogLineParsingException;
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.HttpMethod;

public class AccessLogLineParserTest {

	private final HttpAccessLogLineParser httpAccessLogLineParser = new HttpAccessLogLineParser();

	@Test
	public void testParse() {

		AccessLogLine response = httpAccessLogLineParser.apply("127.0.0.1 loggeduser mary [09/May/2018:16:00:42 +0000] \"POST /api/user HTTP/1.0\" 503 12");

		assertThat(response).isEqualToIgnoringGivenFields(AccessLogLine.builder()
				.host("127.0.0.1")
				.identifier("loggeduser")
				.user("mary")
				.dateTime(ZonedDateTime.of(LocalDateTime.of(2018, 5, 9, 16, 0, 42), ZoneId.of("+0000")).toLocalDateTime())
				.httpMethod(HttpMethod.POST)
				.resource("/api/user")
				.protocol("HTTP/1.0")
				.returnedStatus(503)
				.contentSize(12)
				.build(), "insertTime");
	}

	@Test
	public void insertTimeNotNull() {

		AccessLogLine response = httpAccessLogLineParser.apply("127.0.0.1 loggeduser mary [09/May/2018:16:00:42 +0000] \"POST /api/user HTTP/1.0\" 503 12");

		Assert.assertEquals(0, ChronoUnit.SECONDS.between(response.getInsertTime(), LocalDateTime.now()));
	}

	@Test
	public void testDashedLine() {

		AccessLogLine response = httpAccessLogLineParser.apply("- - - [09/May/2018:16:00:42 +0000] \"POST /api/user HTTP/1.0\" 503 12");

		assertThat(response).isEqualToIgnoringGivenFields(AccessLogLine.builder()
				.host("")
				.identifier("NO_IDENTIFIER")
				.user("NO_USER")
				.dateTime(ZonedDateTime.of(LocalDateTime.of(2018, 5, 9, 16, 0, 42), ZoneId.of("+0000")).toLocalDateTime())
				.httpMethod(HttpMethod.POST)
				.resource("/api/user")
				.protocol("HTTP/1.0")
				.returnedStatus(503)
				.contentSize(12)
				.build(), "insertTime");
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
