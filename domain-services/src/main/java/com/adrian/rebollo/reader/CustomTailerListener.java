package com.adrian.rebollo.reader;

import java.util.concurrent.Executor;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.springframework.stereotype.Component;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.parser.HttpAccessLogLineParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tailer Listener which will be called for every Tailer polling action.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomTailerListener implements TailerListener {

	private final HttpAccessLogLineParser httpAccessLogLineParser;
	private final InternalDispatcher internalDispatcher;
	private final Executor httpLogReaderThreadPool;

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

	/**
	 * This improvement allows to handle lines in parallel by several threads through a configured ThreadPoolTaskExecutor.
	 * LOG Example when processing (Notice Thread Name):
	 * 2020-04-02 10:18:07.247  INFO 45244 --- [torThreadPool-2] c.a.r.parser.HttpAccessLogLineParser     : Proceeding to parse
	 * 2020-04-02 10:18:07.252  INFO 45244 --- [torThreadPool-4] c.a.r.parser.HttpAccessLogLineParser     : Proceeding to parse
	 * 2020-04-02 10:18:07.256  INFO 45244 --- [torThreadPool-6] c.a.r.parser.HttpAccessLogLineParser     : Proceeding to parse
	 * 2020-04-02 10:18:07.259  INFO 45244 --- [torThreadPool-1] c.a.r.parser.HttpAccessLogLineParser     : Proceeding to parse
	 *
	 * Parallel thread process will depend on the number of processors the machine where the service running has.
	 */
	@Override
	public void handle(String line) {
		httpLogReaderThreadPool.execute(() -> internalDispatcher.dispatch(httpAccessLogLineParser.apply(line)));
	}

	@Override
	public void handle(Exception ex) {
		LOG.error("Handling exception in CustomTailerListener", ex);
	}
}
