package com.adrian.rebollo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "adapters.activemq")
public class PrimaryEndpoint {

	private String internalLogLineQueue;
	private String internalLogLineQueueDead;

	private String internalLogStatsQueue;
	private String internalLogStatsQueueDead;

	private String internalLogAlertQueue;
	private String internalLogAlertQueueDead;
}
