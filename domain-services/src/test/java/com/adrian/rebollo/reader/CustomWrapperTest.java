package com.adrian.rebollo.reader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.commons.io.input.Tailer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CustomWrapperTest {

	@InjectMocks
	private TailerWrapper tailerWrapper;

	@Mock
	private Tailer configuredTailer;

	@Before
	public void init() {
		File mockFile = mock(File.class);
		when(mockFile.getAbsolutePath()).thenReturn("");
		when(configuredTailer.getFile()).thenReturn(mockFile);
	}

	@Test
	public void testStartsTailerWhenRun() {

		tailerWrapper.run();
		verify(configuredTailer).run();
	}

	@Test
	public void testOnDestroy() {

		tailerWrapper.destroy();
		verify(configuredTailer).stop();
	}
}
