package com.adrian.rebollo.api;

import com.adrian.rebollo.model.HttpAccessLogLine;

/**
 * LogLine Service interface.
 */
public interface HttpAccessLogLineService {

	void handle(HttpAccessLogLine httpAccessLogLine);
}
