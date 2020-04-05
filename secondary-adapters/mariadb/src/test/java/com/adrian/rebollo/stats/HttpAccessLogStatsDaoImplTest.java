package com.adrian.rebollo.stats;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;

import com.adrian.rebollo.entity.LogStats;
import com.adrian.rebollo.mapper.HttpAccessLogStatsMapper;
import com.adrian.rebollo.model.HttpAccessLogStats;

@RunWith(MockitoJUnitRunner.class)
public class HttpAccessLogStatsDaoImplTest {

	@InjectMocks
	private HttpAccessLogStatsDaoImpl httpAccessLogStatsDao;

	@Mock
	private HttpAccessLogStatsMapper mapper;
	@Mock
	private HttpAccessLogStatsRepository repository;
	@Mock
	private Page<LogStats> mockPage;

	@Test
	public void testNoResultReturnsEmpty() {

		when(repository.findByInsertTimeBetween(any(), any(), any())).thenReturn(Page.empty());
		Assert.assertEquals(Optional.empty(), httpAccessLogStatsDao.findByDateBetween(LocalDateTime.now(), LocalDateTime.now(), 10));
	}

	@Test
	public void testResultCallsMapper() {

		when(mockPage.isEmpty()).thenReturn(false);
		when(mockPage.get()).thenReturn(Stream.of(new LogStats()));
		when(repository.findByInsertTimeBetween(any(), any(), any())).thenReturn(mockPage);

		httpAccessLogStatsDao.findByDateBetween(LocalDateTime.now(), LocalDateTime.now(), 10);

		verify(mapper).toDomain(any(LogStats.class));
	}

	@Test
	public void saveCallsToEntity() {

		httpAccessLogStatsDao.save(new HttpAccessLogStats());

		verify(mapper).toEntity(any(HttpAccessLogStats.class));
	}
}
