package com.adrian.rebollo.api;

import com.adrian.rebollo.model.LogInfo;

/**
 * Abstraction of Observer Pattern to notify all Observers.
 */
public interface ExternalDispatcherObserver {

	/**
	 * notify observers about LogInfo entity.
	 */
	void notify(LogInfo logInfo);
}
