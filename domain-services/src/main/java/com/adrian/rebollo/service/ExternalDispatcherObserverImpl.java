package com.adrian.rebollo.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.adrian.rebollo.api.ExternalDispatcher;
import com.adrian.rebollo.api.ExternalDispatcherObserver;
import com.adrian.rebollo.model.LogInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExternalDispatcherObserverImpl implements ExternalDispatcherObserver {

	/**
	 * injects all implementations of ExternalDispatchers (LogExternalDispatcher and JsonExternalDispatcher at this moment).
	 * If a new ExternalDispatcher implementation would be introduced in the future, it would be automatically notified.
	 * Tipically, in the Observer pattern, this approach is achieved with the `register()` method, but here we can acquire it with IOC.
	 */
	private final List<ExternalDispatcher> observers;

	/**
	 * Notify all implementations of ExternalDispatchers about the new logInfo (stats or alert) to handle it.
	 * @param logInfo to dispatch/notify
	 */
	@Override
	public void notify(LogInfo logInfo) {
		observers.forEach((observer) -> observer.dispatch(logInfo));
	}
}
