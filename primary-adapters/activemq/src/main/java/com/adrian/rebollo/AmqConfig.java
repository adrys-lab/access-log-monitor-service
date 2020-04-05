package com.adrian.rebollo;

import java.util.concurrent.Executor;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AmqConfig {

	@Value("${adapters.activemq.broker-url}")
	private String brokerUrl;
	@Value("${service.thread-pool-size}")
	private int threadPoolSize;
	@Value("${service.max-thread-pool-size}")
	private int maxThreadPoolSize;

	@Bean
	public ActiveMQConnectionFactory senderActiveMQConnectionFactory() {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL(brokerUrl);
		//This allows to consume messages in parallel
		activeMQConnectionFactory.setMaxThreadPoolSize(maxThreadPoolSize);
		return activeMQConnectionFactory;
	}

	@Bean
	public CachingConnectionFactory cachingConnectionFactory() {
		return new CachingConnectionFactory(senderActiveMQConnectionFactory());
	}

	@Bean
	public JmsTemplate jmsTemplate(ActiveMQComponent component) {
		return new JmsTemplate(cachingConnectionFactory());
	}

	@Bean(name = "logConsumerThreadPool")
	public Executor httpLogMonitorThreadPool() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(threadPoolSize);
		executor.setMaxPoolSize(maxThreadPoolSize);
		executor.setQueueCapacity(5000);
		executor.setThreadNamePrefix("LogConsumerThreadPool-");
		executor.initialize();
		return executor;
	}
}
