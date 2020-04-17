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
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.parser.AccessLogLineParser;

@RunWith(MockitoJUnitRunner.class)
public class CustomTailerListenerTest {

	private CustomTailerListener customTailerListener;

	@Mock
	private AccessLogLineParser accessLogLineParser;
	@Mock
	ThreadPoolTaskExecutor executor;
	@Mock
	private InternalDispatcher internalDispatcher;

	@Before
	public void init() {
		customTailerListener = new CustomTailerListener(accessLogLineParser, internalDispatcher, executor);
	}

	@Test
	public void testCalls() {
		final String line = "whatever";
		final AccessLogLine parsed = AccessLogLine.builder().build();

		customTailerListener.handle(line);

		verify(executor).execute(any());
	}
}
