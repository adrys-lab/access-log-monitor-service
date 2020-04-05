package com.adrian.rebollo.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
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
import com.adrian.rebollo.dao.HttpAccessLogLineDao;
import com.adrian.rebollo.dao.HttpAccessLogStatsDao;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpAccessLogStats;

@RunWith(MockitoJUnitRunner.class)
public class HttpAccessLogStatsServiceImplTest {

	@InjectMocks
	private HttpAccessLogStatsServiceImpl httpAccessLogStatsService;

	@Mock
	private InternalDispatcher internalDispatcher;
	@Mock
	private HttpAccessLogLineDao httpAccessLogLineDao;
	@Mock
	private HttpAccessLogStatsDao httpAccessLogStatsDao;
	@Mock
	private HttpAccessLogStatsComponent httpAccessLogStatsComponent;

	@Captor
	private ArgumentCaptor<HttpAccessLogStats> statsCaptor;

	@Before
	public void init() {
		ReflectionTestUtils.setField(httpAccessLogStatsService, "schedulerDelay", 10000);
		ReflectionTestUtils.setField(httpAccessLogStatsService, "chunk", 10);
	}

	@Test
	public void testEmptyResults() {

		httpAccessLogStatsService.aggregate();

		verify(httpAccessLogLineDao).findBySeqIdGreater(eq(Long.MIN_VALUE), eq(10));
		verify(internalDispatcher).dispatch(statsCaptor.capture());
		verifyNoInteractions(httpAccessLogStatsDao);
	}

	@Test
	public void seqIdIsUpdatedWithMax() {

		final HttpAccessLogLine httpAccessLogLine = new HttpAccessLogLine().setSeqId(10L).setInsertTime(LocalDateTime.now());
		final HttpAccessLogLine secondHttpAccessLogLine = new HttpAccessLogLine().setSeqId(20L).setInsertTime(LocalDateTime.now());

		when(httpAccessLogLineDao.findBySeqIdGreater(eq(Long.MIN_VALUE), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine, secondHttpAccessLogLine)));

		httpAccessLogStatsService.aggregate();

		verify(httpAccessLogLineDao).findBySeqIdGreater(eq(Long.MIN_VALUE), eq(10));
		verify(internalDispatcher).dispatch(statsCaptor.capture());
		verify(httpAccessLogStatsDao).save(any(HttpAccessLogStats.class));

		Assert.assertEquals(20L, ((AtomicLong) ReflectionTestUtils.getField(httpAccessLogStatsService, "lastSeqId")).get());
	}

	@Test
	public void callsStatsComponent() {

		final HttpAccessLogLine httpAccessLogLine = new HttpAccessLogLine().setInsertTime(LocalDateTime.now());

		when(httpAccessLogLineDao.findBySeqIdGreater(eq(Long.MIN_VALUE), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogStatsService.aggregate();

		verify(httpAccessLogStatsComponent).compute(any(HttpAccessLogStats.class), eq(httpAccessLogLine));
		verify(httpAccessLogStatsComponent).aggregate(any(HttpAccessLogStats.class));
		verify(httpAccessLogStatsDao).save(any(HttpAccessLogStats.class));
		verify(internalDispatcher).dispatch(any(HttpAccessLogStats.class));
	}

	@Test
	public void setsMaxAndMinDate() {

		LocalDateTime firstDate = LocalDateTime.now().minus(10, ChronoUnit.SECONDS);
		LocalDateTime secondDate = LocalDateTime.now().minus(5, ChronoUnit.SECONDS);;
		LocalDateTime thirdDate = LocalDateTime.now().plus(5, ChronoUnit.SECONDS);;
		LocalDateTime fourthDate = LocalDateTime.now().plus(10, ChronoUnit.SECONDS);;

		final HttpAccessLogLine firstLogLine = new HttpAccessLogLine().setInsertTime(firstDate);
		final HttpAccessLogLine secondLogLine = new HttpAccessLogLine().setInsertTime(secondDate);
		final HttpAccessLogLine thirdLogLine = new HttpAccessLogLine().setInsertTime(thirdDate);
		final HttpAccessLogLine fourthLogLine = new HttpAccessLogLine().setInsertTime(fourthDate);

		when(httpAccessLogLineDao.findBySeqIdGreater(eq(Long.MIN_VALUE), eq(10))).thenReturn(Optional.of(List.of(firstLogLine, secondLogLine, thirdLogLine, fourthLogLine)));

		httpAccessLogStatsService.aggregate();

		verify(internalDispatcher).dispatch(statsCaptor.capture());

		Assert.assertEquals(0, ChronoUnit.SECONDS.between(firstDate, statsCaptor.getValue().getStart()));
		Assert.assertEquals(0, ChronoUnit.SECONDS.between(fourthDate, statsCaptor.getValue().getEnd()));
	}
}
