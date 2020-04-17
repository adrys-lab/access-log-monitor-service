package com.adrian.rebollo.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AccessLogAlert extends LogInfo {

	private long requests;

	private double requestsSecond;

	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
	private LocalDateTime alertTime;

	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
	private LocalDateTime start;

	@JsonSerialize(using = LocalDateTimeSerializer.class)
	@JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
	private LocalDateTime end;

	private AlertType type;

	@Override
	public String toString() {
		return String.format(type.getMessage(), requests, requestsSecond, alertTime, start, end);
	}
}
