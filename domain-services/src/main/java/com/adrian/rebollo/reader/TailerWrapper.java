package com.adrian.rebollo.reader;

import org.apache.commons.io.input.Tailer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * TailerWrapper encapsulates the Tailer instance and allows to start/stop.
 */
@Slf4j
@Component
public class TailerWrapper implements DisposableBean {

	private final Tailer tailer;

	/**
	 * this gets the configuredTailer Bean instance provided by {@link com.adrian.rebollo.DomainServicesConfig}
	 * @param configuredTailer
	 */
	public TailerWrapper(Tailer configuredTailer) {
		this.tailer = configuredTailer;
	}

	/**
	 * Start the Tailer in another Thread with async Spring annotation.
	 */
	@Async
	public void run() {
		LOG.info("Starting TailerWrapper with delay={} for file{}", tailer.getDelay(), tailer.getFile().getAbsolutePath());
		tailer.run();
	}

	/**
	 * when destroying the bean close the Tailer.
	 */
	@Override
	public void destroy() {
		LOG.info("Destroying Bean TailerWrapper, proceed to stop TailerWrapper with delay={} for file{}", tailer.getDelay(), tailer.getFile().getAbsolutePath());
		tailer.stop();
	}
}
