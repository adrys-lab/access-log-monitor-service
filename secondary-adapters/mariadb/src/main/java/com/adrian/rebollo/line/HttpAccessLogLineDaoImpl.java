package com.adrian.rebollo.line;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.dao.HttpAccessLogLineDao;
import com.adrian.rebollo.entity.LogLine;
import com.adrian.rebollo.mapper.HttpAccessLogLineMapper;
import com.adrian.rebollo.model.HttpAccessLogLine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpAccessLogLineDaoImpl implements HttpAccessLogLineDao {

	private final HttpAccessLogLineMapper mapper;
	private final HttpAccessLogLineRepository repository;

	@Override
	public void save(final HttpAccessLogLine httpAccessLogLine) {
		LogLine logLine = mapper.toEntity(httpAccessLogLine);
		repository.saveAndFlush(logLine);
		LOG.debug("Proceeded to save logLine={}", logLine);
	}

	@Override
	public Optional<List<HttpAccessLogLine>> findBySeqIdGreater(long seq, final int chunkSize) {

		LOG.info("Proceeding to find logs by seqId, seq={}, chunk={}", seq, chunkSize);

		final Pageable pageable = PageRequest.of(0, chunkSize, Sort.by("seqId").ascending());
		final Page<LogLine> logPage = repository.findBySeqIdGreaterThanOrderBySeqIdAsc(seq, pageable);

		if (logPage.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(logPage
				.get()
				.map(mapper::toDomain)
				.collect(Collectors.toList()));
	}
}
