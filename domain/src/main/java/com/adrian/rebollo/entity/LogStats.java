package com.adrian.rebollo.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Table(name = "stats")
@Entity(name = "stats")
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LogStats extends PersistedEntity {

	public LogStats(long seq, LocalDateTime insertTime, long requests, long validRequests, long invalidRequests, long totalContent) {
		super(seq, insertTime);
		this.requests = requests;
		this.validRequests = validRequests;
		this.invalidRequests = invalidRequests;
		this.totalContent = totalContent;
	}

	@Column(nullable = false)
	private long requests;
	@Column(name = "valid_requests")
	private long validRequests;
	@Column(name = "invalid_requests")
	private long invalidRequests;
	@Column(name = "total_content")
	private long totalContent;
}
