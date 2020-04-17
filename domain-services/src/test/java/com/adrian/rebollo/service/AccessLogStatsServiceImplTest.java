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
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.AccessLogStats;

@RunWith(MockitoJUnitRunner.class)
public class AccessLogStatsServiceImplTest {

	@InjectMocks
	private AccessLogStatsServiceImpl httpAccessLogStatsService;

	@Mock
	private InternalDispatcher internalDispatcher;
	@Mock
	private AccessLogStatsComponent accessLogStatsComponent;

	@Captor
	private ArgumentCaptor<AccessLogStats> statsCaptor;

	@Before
	public void init() {
		ReflectionTestUtils.setField(httpAccessLogStatsService, "schedulerDelay", 10000);
	}

	@Test
	public void testEmptyResults() {

		httpAccessLogStatsService.aggregate();

		verify(internalDispatcher).dispatch(statsCaptor.capture());
	}

	@Test
	public void callsStatsComponent() {

		final AccessLogLine accessLogLine = new AccessLogLine().setInsertTime(LocalDateTime.now());

		when(accessLogStatsComponent.aggregateLogs(anyList())).thenReturn(new AccessLogStats());

		httpAccessLogStatsService.handle(accessLogLine);
		httpAccessLogStatsService.aggregate();

		verify(accessLogStatsComponent).aggregateLogs(eq(List.of(accessLogLine)));
		verify(internalDispatcher).dispatch(any(AccessLogStats.class));
	}
}
