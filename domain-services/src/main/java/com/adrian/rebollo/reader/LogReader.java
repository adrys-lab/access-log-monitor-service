package com.adrian.rebollo.reader;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LogReader {

	private final TailerWrapper tailerWrapper;

	@PostConstruct
	public void init() {
		tailerWrapper.run();
	}
}
