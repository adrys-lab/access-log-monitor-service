package com.adrian.rebollo.api;

import com.adrian.rebollo.model.HttpAccessLogAlert;
import com.adrian.rebollo.model.HttpAccessLogLine;
import com.adrian.rebollo.model.HttpAccessLogStats;

/**
 * Main abstraction for dispatch entities internally across the service.
 */
public interface InternalDispatcher {

	/**
	 * dispatch the given httpAccessLogLine to be processed by its subscribers.
	 *
	 * @param httpAccessLogLine to dispatch internally
	 */
	void dispatch(HttpAccessLogLine httpAccessLogLine);

	/**
	 * dispatch the given httpAccessLogStats to be processed by its subscribers.
	 *
	 * @param httpAccessLogStats to dispatch internally
	 */
	void dispatch(HttpAccessLogStats httpAccessLogStats);

	/**
	 * dispatch the given httpAccessAlert to be processed by its subscribers.
	 *
	 * @param httpAccessLogAlert to dispatch internally
	 */
	void dispatch(HttpAccessLogAlert httpAccessLogAlert);
}
