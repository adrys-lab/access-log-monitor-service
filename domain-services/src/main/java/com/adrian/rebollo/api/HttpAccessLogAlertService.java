package com.adrian.rebollo.api;

import com.adrian.rebollo.model.HttpAccessLogStats;

/**
 * LogLine Service interface.
 */
public interface HttpAccessLogAlertService {

	void handle(HttpAccessLogStats httpAccessLogStats);
}
