package com.adrian.rebollo.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.adrian.rebollo.model.LogInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonDispatcherComponent {

	@Value("${adapters.json.file-name}")
	private String fileName;

	private final ObjectMapper objectMapper;

	/**
	 * Creates a new Json file.
	 */
	void createNewJson() {
		try(FileOutputStream fileOutputStream = getOutputStream()) {
			objectMapper.writeValue(fileOutputStream, new JsonInfo());
			fileOutputStream.flush();
		} catch (IOException exception) {
			LOG.error(String.format("Could not write object into %s", fileName), exception);
		}
	}

	/**
	 * writes a new objecct into the json file.
	 * we need to synchronize this method as the outputstream and inputstream need to be free and closed before get it. --> file access needs to be one thread at time.
	 */
	synchronized void writeObjectToJson(final LogInfo logInfo) {

		final JsonInfo jsonInfo = readJsonInfo();

		try(FileOutputStream fileOutputStream = getOutputStream()) {
			jsonInfo.getResult().add(logInfo);
			objectMapper.writeValue(fileOutputStream, jsonInfo);
		} catch (IOException exception) {
			LOG.error(String.format("Could not write object to json %s", fileName), exception);
		}
	}

	/**
	 * read the current data from the json file.
	 */
	private JsonInfo readJsonInfo() {
		try(FileInputStream fileInputStream = getInputStream()) {
			return objectMapper.readValue(fileInputStream, JsonInfo.class);
		} catch (IOException exception) {
			LOG.error(String.format("Could not read object from json %s", fileName), exception);
		}
		return new JsonInfo();
	}

	private FileInputStream getInputStream() throws IOException {
		return FileUtils.openInputStream((new File(fileName)));
	}

	private FileOutputStream getOutputStream() throws IOException {
		return FileUtils.openOutputStream(new File(fileName));
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	private static class JsonInfo {

		/**
		 * This list allows to have all the objects in the same list.
		 * with this approach, all the elements are ordered by insertion, so its easier to debug than having 2 lists (1 for stats and 1 for alerts) independently.
		 */
		@JsonProperty("result")
		private List<Object> result = new LinkedList<>();
	}
}
