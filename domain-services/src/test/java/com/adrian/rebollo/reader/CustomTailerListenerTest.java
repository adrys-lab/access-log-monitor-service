package com.adrian.rebollo.reader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.parser.HttpAccessLogLineParser;

@RunWith(MockitoJUnitRunner.class)
public class CustomTailerListenerTest {

	private CustomTailerListener customTailerListener;

	@Mock
	private HttpAccessLogLineParser httpAccessLogLineParser;
	@Mock
	ThreadPoolTaskExecutor executor;
	@Mock
	private InternalDispatcher internalDispatcher;

	@Before
	public void init() {
		customTailerListener = new CustomTailerListener(httpAccessLogLineParser, internalDispatcher, executor);
	}

	@Test
	public void testCalls() {
		final String line = "whatever";
		final HttpAccessLogLine parsed = HttpAccessLogLine.builder().build();

		customTailerListener.handle(line);

		verify(executor).execute(any());
	}
}