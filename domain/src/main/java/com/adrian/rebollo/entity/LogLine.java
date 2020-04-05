package com.adrian.rebollo.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Table(name = "log")
@Entity(name = "log")
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LogLine extends PersistedEntity {

	public LogLine(long seq, LocalDateTime insertTime, String line, String host, String identifier, String user, LocalDateTime dateTime,
			String httpMethod, String resource, String protocol, int returnedStatus, long contentSize) {
		super(seq, insertTime);
		this.line = line;
		this.host = host;
		this.identifier = identifier;
		this.user = user;
		this.dateTime = dateTime;
		this.httpMethod = httpMethod;
		this.resource = resource;
		this.protocol = protocol;
		this.returnedStatus = returnedStatus;
		this.contentSize = contentSize;
	}

	@Column(nullable = false)
	private String line;
	@Column
	private String host;
	@Column
	private String identifier;
	@Column(name = "general_user")
	private String user;
	@Column(name = "date_time")
	@Convert(converter = Jsr310JpaConverters.LocalDateTimeConverter.class)
	private LocalDateTime dateTime;
	@Column(name = "http_method")
	private String httpMethod;
	@Column
	private String resource;
	@Column
	private String protocol;
	@Column(name = "returned_status")
	private int returnedStatus;
	@Column(name = "content_size")
	private long contentSize;
}
