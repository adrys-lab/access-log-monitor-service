package com.adrian.rebollo.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpAccessLogLine {

	private String line;

	private long seqId;

	@JsonDeserialize(using = LocalDateTimeDeserializer.class)
	@JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
	private LocalDateTime insertTime;

	private String host;

	private String identifier;

	private String user;

	private LocalDateTime dateTime;

	private HttpMethod httpMethod;

	private String resource;

	private String protocol;

	private int returnedStatus;

	private long contentSize;
}
