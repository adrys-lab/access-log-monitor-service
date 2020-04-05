package com.adrian.rebollo.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Helper class for helping on routing internal AMQ bindings.
 */
public class ActiveMqDestinationBuilder {

	public static ActiveMqDestinationBuilder queue(String queue) {
		return new ActiveMqDestinationBuilder(queue, "queue");
	}

	public static ActiveMqDestinationBuilder topic(String topic) {
		return new ActiveMqDestinationBuilder(topic, "topic");
	}

	private final String destination;
	private final String destinationType;
	private final Map<String, Object> options;

	private ActiveMqDestinationBuilder(String destination, String destinationType) {
		this.destination = destination;
		this.destinationType = destinationType;
		this.options = new HashMap<>();
	}

	public ActiveMqDestinationBuilder setOption(String key, Object value) {
		this.options.put(key, value);
		return this;
	}

	public ActiveMqDestinationBuilder setTransacted(boolean transacted) {
		return this.setOption("transacted", transacted);
	}

	public ActiveMqDestinationBuilder setConcurrentConsumers(int concurrentConsumers) {
		return this.setOption("concurrentConsumers", concurrentConsumers);
	}

	public ActiveMqDestinationBuilder setMaxConcurrentConsumers(int maxConcurrentConsumers) {
		return this.setOption("maxConcurrentConsumers", maxConcurrentConsumers);
	}

	public String build() {
		StringJoiner optionsString = new StringJoiner("&");
		this.options.forEach((key, value) -> optionsString.add(key + "=" + value));
		return (this.options.isEmpty())
				? ("activemq:" + this.destinationType + ":" + this.destination)
				: ("activemq:" + this.destinationType + ":" + this.destination + "?" + optionsString);
	}

}
