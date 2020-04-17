package com.adrian.rebollo.api;

import com.adrian.rebollo.model.AccessLogLine;

/**
 * Log Stats Service interface.
 */
public interface AccessLogStatsService {

	/**
	 * Log Stats Service handles Log Line.
	 * @param accessLogLine to handle
	 */
	void handle(AccessLogLine accessLogLine);
}
