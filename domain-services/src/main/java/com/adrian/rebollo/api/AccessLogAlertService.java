package com.adrian.rebollo.api;

import com.adrian.rebollo.model.AccessLogStats;

/**
 * Log Alert Service interface.
 */
public interface AccessLogAlertService {

	/**
	 * Log Alert Service handles Log Stats.
	 * @param accessLogStats to handle
	 */
	void handle(AccessLogStats accessLogStats);
}
