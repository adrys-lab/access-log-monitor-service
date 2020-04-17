package com.adrian.rebollo.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.AccessLogStats;
import com.adrian.rebollo.model.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LogStatsComponentTest {

	private final AccessLogStatsComponent accessLogStatsComponent = new AccessLogStatsComponent();

	@Test
	public void testCompute() {

		final AccessLogLine logLine = AccessLogLine.builder()
				.insertTime(LocalDateTime.now())
				.host("127.0.0.1")
				.identifier("user1")
				.user("mary")
				.httpMethod(HttpMethod.PATCH)
				.resource("/api/user")
				.returnedStatus(503)
				.contentSize(12)
				.build();

		final AccessLogStats accessLogStats = accessLogStatsComponent.aggregateLogs(List.of(logLine));

		final AccessLogStats expected = new AccessLogStats()
				.setRequests(new AtomicLong(1))
				.setValidRequests(new AtomicLong(0))
				.setInvalidRequests(new AtomicLong(1))
				.setTotalContent(new AtomicLong(logLine.getContentSize()));

		expected.getTopVisitsByHost().put("127.0.0.1", new AtomicLong(1L));
		expected.getTopVisitsByUser().put("mary", new AtomicLong(1L));
		expected.getTopVisitsByMethod().put(HttpMethod.PATCH.name(), new AtomicLong(1L));
		expected.getTopInvalidVisitedRequestsSections().put("/api", new AtomicLong(1L));
		expected.getTopVisitsSection().put("/api", new AtomicLong(1L));

		assertEquals(expected, accessLogStats);
	}

	@Test
	public void testComputeSeveral() {

		final AccessLogLine logLine = AccessLogLine.builder()
				.insertTime(LocalDateTime.now())
				.host("127.0.0.1")
				.identifier("user1")
				.user("marc")
				.httpMethod(HttpMethod.GET)
				.resource("/metrics/products")
				.returnedStatus(201)
				.contentSize(10)
				.build();

		final AccessLogLine secondLogLine = AccessLogLine.builder()
				.insertTime(LocalDateTime.now())
				.host("127.0.0.2")
				.identifier("user1")
				.user("mary")
				.httpMethod(HttpMethod.PUT)
				.resource("/metrics/user/buys")
				.returnedStatus(200)
				.contentSize(20)
				.build();

		final AccessLogLine thirdLogLine = AccessLogLine.builder()
				.insertTime(LocalDateTime.now())
				.host("127.0.0.3")
				.identifier("user1")
				.user("marc")
				.httpMethod(HttpMethod.GET)
				.resource("/endpoint/user")
				.returnedStatus(404)
				.contentSize(30)
				.build();

		final AccessLogStats accessLogStats = accessLogStatsComponent.aggregateLogs(List.of(logLine, secondLogLine, thirdLogLine));

		final AccessLogStats expected = new AccessLogStats()
				.setRequests(new AtomicLong(3))
				.setValidRequests(new AtomicLong(2))
				.setInvalidRequests(new AtomicLong(1))
				.setTotalContent(new AtomicLong(60));

		expected.getTopVisitsByHost().putAll(Map.of("127.0.0.1", new AtomicLong(1L), "127.0.0.2", new AtomicLong(1L), "127.0.0.3", new AtomicLong(1L)));
		expected.getTopVisitsByUser().putAll(Map.of("marc", new AtomicLong(2L), "mary", new AtomicLong(1L)));
		expected.getTopVisitsByMethod().putAll(Map.of(HttpMethod.PUT.name(), new AtomicLong(1L), HttpMethod.GET.name(), new AtomicLong(2L)));
		expected.getTopValidVisitedRequestsSections().putAll(Map.of("/metrics", new AtomicLong(2L)));
		expected.getTopInvalidVisitedRequestsSections().putAll(Map.of("/endpoint", new AtomicLong(1L)));
		expected.getTopVisitsSection().putAll(Map.of("/metrics", new AtomicLong(2L), "/endpoint", new AtomicLong(1L)));

		assertEquals(expected, accessLogStats);
	}

	@Test
	public void setsMaxAndMinDate() {

		LocalDateTime firstDate = LocalDateTime.now().minus(10, ChronoUnit.SECONDS);
		LocalDateTime lastDate = LocalDateTime.now().plus(10, ChronoUnit.SECONDS);

		final AccessLogLine logLine = AccessLogLine.builder()
				.insertTime(firstDate)
				.host("127.0.0.1")
				.identifier("user1")
				.user("marc")
				.httpMethod(HttpMethod.GET)
				.resource("/metrics/products")
				.returnedStatus(201)
				.contentSize(10)
				.build();

		final AccessLogLine secondLogLine = AccessLogLine.builder()
				.insertTime(LocalDateTime.now())
				.host("127.0.0.2")
				.identifier("user1")
				.user("mary")
				.httpMethod(HttpMethod.PUT)
				.resource("/metrics/user/buys")
				.returnedStatus(200)
				.contentSize(20)
				.build();

		final AccessLogLine thirdLogLine = AccessLogLine.builder()
				.insertTime(lastDate)
				.host("127.0.0.3")
				.identifier("user1")
				.user("marc")
				.httpMethod(HttpMethod.GET)
				.resource("/endpoint/user")
				.returnedStatus(404)
				.contentSize(30)
				.build();

		final AccessLogStats accessLogStats = accessLogStatsComponent.aggregateLogs(List.of(logLine, secondLogLine, thirdLogLine));

		Assert.assertEquals(0, ChronoUnit.SECONDS.between(firstDate, accessLogStats.getStart()));
		Assert.assertEquals(0, ChronoUnit.SECONDS.between(lastDate, accessLogStats.getEnd()));
	}

	@Test
	public void testAggregation() throws IOException {


		final Logs logs = new ObjectMapper().readValue(getClass().getResource("/logLines.json"), Logs.class);

		final AccessLogStats accessLogStats = accessLogStatsComponent.aggregateLogs(logs.logs);

		final AccessLogStats expected = new AccessLogStats()
				.setRequests(new AtomicLong(20))
				.setValidRequests(new AtomicLong(12))
				.setInvalidRequests(new AtomicLong(8))
				.setTotalContent(new AtomicLong(400));

		expected.getTopVisitsByHost().putAll(Map.of(
				"127.0.0.2", new AtomicLong(5L),
				"127.0.0.1", new AtomicLong(5L),
				"127.0.0.3", new AtomicLong(4L),
				"127.0.0.9", new AtomicLong(1L),
				"127.0.0.8", new AtomicLong(1L),
				"127.0.0.7", new AtomicLong(1L),
				"127.0.0.6", new AtomicLong(1L),
				"127.0.0.5", new AtomicLong(1L),
				"127.0.0.4", new AtomicLong(1L)
		));

		expected.getTopVisitsByUser().putAll(Map.of(
				"mary", new AtomicLong(10L),
				"marc", new AtomicLong(5L),
				"pol", new AtomicLong(4L),
				"adrian", new AtomicLong(1L))
		);
		expected.getTopVisitsByMethod().putAll(Map.of(
				HttpMethod.GET.name(), new AtomicLong(10L),
				HttpMethod.PATCH.name(), new AtomicLong(4L),
				HttpMethod.PUT.name(), new AtomicLong(3L),
				HttpMethod.POST.name(), new AtomicLong(3L))
		);

		expected.getTopInvalidVisitedRequestsSections().putAll(Map.of(
				"/metrics", new AtomicLong(4L),
				"/employees", new AtomicLong(3L),
				"/users", new AtomicLong(1L)
		));
		expected.getTopVisitsByHost().putAll(Map.of(
				"127.0.0.3", new AtomicLong(5L),
				"127.0.0.2", new AtomicLong(5L),
				"127.0.0.1", new AtomicLong(4L),
				"127.0.0.9", new AtomicLong(1L),
				"127.0.0.8", new AtomicLong(1L),
				"127.0.0.7", new AtomicLong(1L),
				"127.0.0.6", new AtomicLong(1L),
				"127.0.0.5", new AtomicLong(1L),
				"127.0.0.4", new AtomicLong(1L)
		));
		expected.getTopVisitsByUser().putAll(Map.of(
				"mary", new AtomicLong(10L),
				"adrian", new AtomicLong(5L),
				"pol", new AtomicLong(4L),
				"marc", new AtomicLong(1L)
		));
		expected.getTopVisitsSection().putAll(Map.of(
				"/employees", new AtomicLong(5L),
				"/metrics", new AtomicLong(8L),
				"/users", new AtomicLong(4L),
				"/persons", new AtomicLong(1L),
				"/buys", new AtomicLong(2L)
		));

		assertEquals(expected, accessLogStats);
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static class Logs {
		private List<AccessLogLine> logs;
	}

	private void assertEquals(AccessLogStats expected, AccessLogStats accessLogStats) {
		Assert.assertEquals(expected.getRequests().get(), accessLogStats.getRequests().get());
		Assert.assertEquals(expected.getValidRequests().get(), accessLogStats.getValidRequests().get());
		Assert.assertEquals(expected.getInvalidRequests().get(), accessLogStats.getInvalidRequests().get());
		Assert.assertEquals(expected.getTotalContent().get(), accessLogStats.getTotalContent().get());
		//keys
		Assert.assertEquals(expected.getTopVisitsByHost().keySet(), accessLogStats.getTopVisitsByHost().keySet());
		Assert.assertEquals(expected.getTopVisitsByUser().keySet(), accessLogStats.getTopVisitsByUser().keySet());
		Assert.assertEquals(expected.getTopVisitsByMethod().keySet(), accessLogStats.getTopVisitsByMethod().keySet());
		Assert.assertEquals(expected.getTopInvalidVisitedRequestsSections().keySet(), accessLogStats.getTopInvalidVisitedRequestsSections().keySet());
		Assert.assertEquals(expected.getTopVisitsSection().keySet(), accessLogStats.getTopVisitsSection().keySet());
		//values
		Assert.assertEquals(expected.getTopVisitsByHost().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), accessLogStats
				.getTopVisitsByHost().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
		Assert.assertEquals(expected.getTopVisitsByUser().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), accessLogStats
				.getTopVisitsByUser().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
		Assert.assertEquals(expected.getTopVisitsByMethod().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), accessLogStats
				.getTopVisitsByMethod().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
		Assert.assertEquals(expected.getTopInvalidVisitedRequestsSections().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), accessLogStats
				.getTopInvalidVisitedRequestsSections().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
		Assert.assertEquals(expected.getTopVisitsSection().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), accessLogStats
				.getTopVisitsSection().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
	}
}
