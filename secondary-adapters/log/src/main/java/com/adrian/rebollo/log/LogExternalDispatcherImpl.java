package com.adrian.rebollo.log;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.api.ExternalDispatcher;
import com.adrian.rebollo.model.LogInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty( "adapters.log.enabled" )
public class LogExternalDispatcherImpl implements ExternalDispatcher {

	@Override
	public void dispatch(LogInfo logInfo) {
		LOG.info(logInfo.toString());
	}
}
