package com.adrian.rebollo.api;

import com.adrian.rebollo.model.LogInfo;

/**
 * Log Service interface.
 */
public interface ExternalDispatcher {

	/**
	 * dispatch a LogInfo.
	 * (currently Extended class by AccessLogAlert and AccessLogStats)
	 */
	void dispatch(LogInfo logInfo);
}
