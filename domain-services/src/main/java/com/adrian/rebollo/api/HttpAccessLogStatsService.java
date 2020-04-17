package com.adrian.rebollo.api;

import com.adrian.rebollo.model.AccessLogLine;

/**
 * LogLine Service interface.
 */
public interface HttpAccessLogStatsService {

	void handle(AccessLogLine accessLogLine);
}
