package com.adrian.rebollo.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpAccessLogStats;

@RunWith(MockitoJUnitRunner.class)
public class HttpAccessLogStatsServiceImplTest {

	@InjectMocks
	private HttpAccessLogStatsServiceImpl httpAccessLogStatsService;

	@Mock
	private InternalDispatcher internalDispatcher;
	@Mock
	private HttpAccessLogStatsComponent httpAccessLogStatsComponent;

	@Captor
	private ArgumentCaptor<HttpAccessLogStats> statsCaptor;

	@Before
	public void init() {
		ReflectionTestUtils.setField(httpAccessLogStatsService, "schedulerDelay", 10000);
	}

	@Test
	public void testEmptyResults() {

		ReflectionTestUtils.invokeMethod(httpAccessLogStatsService, "aggregate");

		verify(internalDispatcher).dispatch(statsCaptor.capture());
	}

	@Test
	public void callsStatsComponent() {

		final HttpAccessLogLine httpAccessLogLine = new HttpAccessLogLine().setInsertTime(LocalDateTime.now());

		when(httpAccessLogStatsComponent.aggregateLogs(anyList())).thenReturn(new HttpAccessLogStats());

		httpAccessLogStatsService.handle(httpAccessLogLine);
		ReflectionTestUtils.invokeMethod(httpAccessLogStatsService, "aggregate");

		verify(httpAccessLogStatsComponent).aggregateLogs(eq(List.of(httpAccessLogLine)));
		verify(internalDispatcher).dispatch(any(HttpAccessLogStats.class));
	}
}
