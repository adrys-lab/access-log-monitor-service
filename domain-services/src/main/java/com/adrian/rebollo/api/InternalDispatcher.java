package com.adrian.rebollo.api;

import com.adrian.rebollo.model.AccessLogAlert;
import com.adrian.rebollo.model.AccessLogLine;
import com.adrian.rebollo.model.AccessLogStats;

/**
 * Main abstraction for dispatch entities internally across the service.
 */
public interface InternalDispatcher {

	/**
	 * dispatch the given httpAccessLogLine to be processed by its subscribers.
	 *
	 * @param accessLogLine to dispatch internally
	 */
	void dispatch(AccessLogLine accessLogLine);

	/**
	 * dispatch the given httpAccessLogStats to be processed by its subscribers.
	 *
	 * @param accessLogStats to dispatch internally
	 */
	void dispatch(AccessLogStats accessLogStats);

	/**
	 * dispatch the given httpAccessAlert to be processed by its subscribers.
	 *
	 * @param accessLogAlert to dispatch internally
	 */
	void dispatch(AccessLogAlert accessLogAlert);
}
