package com.adrian.rebollo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.adrian.rebollo.api.HttpAccessLogLineService;
import com.adrian.rebollo.dao.HttpAccessLogLineDao;
import com.adrian.rebollo.model.HttpAccessLogLine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpAccessLogLineServiceImpl implements HttpAccessLogLineService {

	private final HttpAccessLogLineDao httpAccessLogLineDao;

	/**
	 *This service only handle Log Lines to be saved.
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handle(HttpAccessLogLine httpAccessLogLine) {
		httpAccessLogLineDao.save(httpAccessLogLine);
	}
}