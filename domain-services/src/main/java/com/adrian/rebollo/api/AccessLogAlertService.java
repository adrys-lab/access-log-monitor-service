package com.adrian.rebollo.api;

import com.adrian.rebollo.model.AccessLogStats;

/**
 * LogLine Service interface.
 */
public interface AccessLogAlertService {

	void handle(AccessLogStats accessLogStats);
}
