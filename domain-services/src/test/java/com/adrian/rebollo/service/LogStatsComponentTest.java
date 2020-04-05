package com.adrian.rebollo.service;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpAccessLogStats;
import com.adrian.rebollo.model.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LogStatsComponentTest {

	private final HttpAccessLogStatsComponent httpAccessLogStatsComponent = new HttpAccessLogStatsComponent();

	@Test
	public void testCompute() {

		final HttpAccessLogStats httpAccessLogStats = new HttpAccessLogStats();

		final HttpAccessLogLine logLine = HttpAccessLogLine.builder()
				.host("127.0.0.1")
				.identifier("user1")
				.user("mary")
				.httpMethod(HttpMethod.PATCH)
				.resource("/api/user")
				.returnedStatus(503)
				.contentSize(12)
				.build();

		httpAccessLogStatsComponent.compute(httpAccessLogStats, logLine);

		final HttpAccessLogStats expected = new HttpAccessLogStats()
				.setRequests(new AtomicLong(1))
				.setValidRequests(new AtomicLong(0))
				.setInvalidRequests(new AtomicLong(1))
				.setTotalContent(new AtomicLong(logLine.getContentSize()));

		expected.getTopVisitsByHost().put("127.0.0.1", new AtomicLong(1L));
		expected.getTopVisitsByUser().put("mary", new AtomicLong(1L));
		expected.getTopVisitsByMethod().put(HttpMethod.PATCH.name(), new AtomicLong(1L));
		expected.getTopInvalidVisitedRequestsSections().put("/api", new AtomicLong(1L));
		expected.getTopVisitsSection().put("/api", new AtomicLong(1L));

		assertEquals(expected, httpAccessLogStats);
	}

	@Test
	public void testComputeSeveral() {

		final HttpAccessLogStats httpAccessLogStats = new HttpAccessLogStats();

		final HttpAccessLogLine logLine = HttpAccessLogLine.builder()
				.host("127.0.0.1")
				.identifier("user1")
				.user("marc")
				.httpMethod(HttpMethod.GET)
				.resource("/metrics/products")
				.returnedStatus(201)
				.contentSize(10)
				.build();

		final HttpAccessLogLine secondLogLine = HttpAccessLogLine.builder()
				.host("127.0.0.2")
				.identifier("user1")
				.user("mary")
				.httpMethod(HttpMethod.PUT)
				.resource("/metrics/user/buys")
				.returnedStatus(200)
				.contentSize(20)
				.build();

		final HttpAccessLogLine thirdLogLine = HttpAccessLogLine.builder()
				.host("127.0.0.3")
				.identifier("user1")
				.user("marc")
				.httpMethod(HttpMethod.GET)
				.resource("/endpoint/user")
				.returnedStatus(404)
				.contentSize(30)
				.build();

		httpAccessLogStatsComponent.compute(httpAccessLogStats, logLine);
		httpAccessLogStatsComponent.compute(httpAccessLogStats, secondLogLine);
		httpAccessLogStatsComponent.compute(httpAccessLogStats, thirdLogLine);

		final HttpAccessLogStats expected = new HttpAccessLogStats()
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

		assertEquals(expected, httpAccessLogStats);
	}

	@Test
	public void testAggregation() throws IOException {

		final HttpAccessLogStats httpAccessLogStats = new HttpAccessLogStats();

		final Logs logs = new ObjectMapper().readValue(getClass().getResource("/logLines.json"), Logs.class);

		logs.logs.forEach((log) -> httpAccessLogStatsComponent.compute(httpAccessLogStats, log));

		httpAccessLogStatsComponent.aggregate(httpAccessLogStats);

		final HttpAccessLogStats expected = new HttpAccessLogStats()
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

		assertEquals(expected, httpAccessLogStats);
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	private static class Logs {
		private List<HttpAccessLogLine> logs;
	}

	private void assertEquals(HttpAccessLogStats expected, HttpAccessLogStats httpAccessLogStats) {
		Assert.assertEquals(expected.getRequests().get(), httpAccessLogStats.getRequests().get());
		Assert.assertEquals(expected.getValidRequests().get(), httpAccessLogStats.getValidRequests().get());
		Assert.assertEquals(expected.getInvalidRequests().get(), httpAccessLogStats.getInvalidRequests().get());
		Assert.assertEquals(expected.getTotalContent().get(), httpAccessLogStats.getTotalContent().get());
		//keys
		Assert.assertEquals(expected.getTopVisitsByHost().keySet(), httpAccessLogStats.getTopVisitsByHost().keySet());
		Assert.assertEquals(expected.getTopVisitsByUser().keySet(), httpAccessLogStats.getTopVisitsByUser().keySet());
		Assert.assertEquals(expected.getTopVisitsByMethod().keySet(), httpAccessLogStats.getTopVisitsByMethod().keySet());
		Assert.assertEquals(expected.getTopInvalidVisitedRequestsSections().keySet(), httpAccessLogStats.getTopInvalidVisitedRequestsSections().keySet());
		Assert.assertEquals(expected.getTopVisitsSection().keySet(), httpAccessLogStats.getTopVisitsSection().keySet());
		//values
		Assert.assertEquals(expected.getTopVisitsByHost().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), httpAccessLogStats.getTopVisitsByHost().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
		Assert.assertEquals(expected.getTopVisitsByUser().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), httpAccessLogStats.getTopVisitsByUser().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
		Assert.assertEquals(expected.getTopVisitsByMethod().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), httpAccessLogStats.getTopVisitsByMethod().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
		Assert.assertEquals(expected.getTopInvalidVisitedRequestsSections().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), httpAccessLogStats.getTopInvalidVisitedRequestsSections().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
		Assert.assertEquals(expected.getTopVisitsSection().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()), httpAccessLogStats.getTopVisitsSection().values().stream().map(AtomicLong::get).sorted(Comparator.comparingLong(left -> left)).collect(Collectors.toList()));
	}
}
