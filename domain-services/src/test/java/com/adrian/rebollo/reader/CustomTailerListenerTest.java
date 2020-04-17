package com.adrian.rebollo.reader;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.parser.AccessLogLineParser;

@RunWith(MockitoJUnitRunner.class)
public class CustomTailerListenerTest {

	private CustomTailerListener customTailerListener;

	@Mock
	private AccessLogLineParser accessLogLineParser;
	@Mock
	private InternalDispatcher internalDispatcher;

	@Before
	public void init() {
		customTailerListener = new CustomTailerListener(accessLogLineParser, internalDispatcher);
	}

	@Test
	public void testCalls() {
		final String line = "whatever";
		final AccessLogLine parsed = AccessLogLine.builder().build();

		when(accessLogLineParser.apply(line)).thenReturn(parsed);

		customTailerListener.handle(line);

		verify(accessLogLineParser).apply(eq(line));
		verify(internalDispatcher).dispatch(eq(parsed));
	}
}
