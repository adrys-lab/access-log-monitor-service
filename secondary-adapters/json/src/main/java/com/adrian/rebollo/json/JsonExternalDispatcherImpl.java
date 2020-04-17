package com.adrian.rebollo.json;

import java.io.File;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.api.ExternalDispatcher;
import com.adrian.rebollo.model.LogInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty( "adapters.json.enabled" )
public class JsonExternalDispatcherImpl implements ExternalDispatcher {

	@Value("${adapters.json.file-name}")
	private String fileName;

	private final JsonDispatcherComponent jsonDispatcherComponent;

	@PostConstruct
	public void init() {
		final File json = FileUtils.getFile(fileName);
		// if the json is empty, create a new json file.
		if(json.length() == 0) {
			jsonDispatcherComponent.createNewJson();
		}
	}

	@Override
	public void dispatch(final LogInfo logInfo) {
		jsonDispatcherComponent.writeObjectToJson(logInfo);
	}
}
