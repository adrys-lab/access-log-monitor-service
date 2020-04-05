package com.adrian.rebollo.line;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;

import com.adrian.rebollo.entity.LogLine;
import com.adrian.rebollo.mapper.HttpAccessLogLineMapper;
import com.adrian.rebollo.model.HttpAccessLogLine;

@RunWith(MockitoJUnitRunner.class)
public class HttpAccessLogLineDaoImplTest {

	@InjectMocks
	private HttpAccessLogLineDaoImpl httpAccessLogLineDao;

	@Mock
	private HttpAccessLogLineMapper mapper;
	@Mock
	private HttpAccessLogLineRepository repository;
	@Mock
	private Page<LogLine> mockPage;

	@Test
	public void testNoResultReturnsEmpty() {

		when(repository.findBySeqIdGreaterThanOrderBySeqIdAsc(eq(1L), any())).thenReturn(Page.empty());
		Assert.assertEquals(Optional.empty(), httpAccessLogLineDao.findBySeqIdGreater(1L, 10));
	}

	@Test
	public void testResultCallsMapper() {

		when(mockPage.isEmpty()).thenReturn(false);
		when(mockPage.get()).thenReturn(Stream.of(new LogLine()));
		when(repository.findBySeqIdGreaterThanOrderBySeqIdAsc(eq(1L), any())).thenReturn(mockPage);

		httpAccessLogLineDao.findBySeqIdGreater(1L, 10);

		verify(mapper).toDomain(any(LogLine.class));
	}

	@Test
	public void saveCallsToEntity() {

		httpAccessLogLineDao.save(new HttpAccessLogLine());

		verify(mapper).toEntity(any(HttpAccessLogLine.class));
	}
}
