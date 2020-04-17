package com.adrian.rebollo.reader;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.springframework.stereotype.Component;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.parser.AccessLogLineParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tailer Listener which will be called for every Tailer polling action.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomTailerListener implements TailerListener {

	private final AccessLogLineParser accessLogLineParser;
	private final InternalDispatcher internalDispatcher;

	@Override
	public void init(Tailer tailer) {
		LOG.info("Initializing CustomTailerListener");
	}

	@Override
	public void fileNotFound() {
		LOG.error("Error file not found in CustomTailerListener");
	}

	@Override
	public void fileRotated() {
		LOG.error("fileRotated in CustomTailerListener");
	}

	@Override
	public void handle(String line) {
		internalDispatcher.dispatch(accessLogLineParser.apply(line));
	}

	@Override
	public void handle(Exception ex) {
		LOG.error("Handling exception in CustomTailerListener", ex);
	}
}
