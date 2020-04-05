package com.adrian.rebollo.model;

import java.time.LocalDateTime;

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
