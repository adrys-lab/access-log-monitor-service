package com.adrian.rebollo.api;

import com.adrian.rebollo.model.AccessLogStats;

/**
 * LogLine Service interface.
 */
public interface HttpAccessLogAlertService {

	void handle(AccessLogStats accessLogStats);
}
