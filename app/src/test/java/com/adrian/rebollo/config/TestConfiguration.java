package com.adrian.rebollo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ActiveProfiles("dev")
@EnableTransactionManagement
public class TestConfiguration {

	@Value("${adapters.activemq.internal-log-stats-queue}")
	private String statsQueue;

	@Bean
	public SimpleJdbcInsert simpleJdbcInsert(JdbcTemplate jdbcTemplate) {
		return new SimpleJdbcInsert(jdbcTemplate);
	}

	@Bean
	public MessageListenerContainer listenerContainer(CachingConnectionFactory cachingConnectionFactory,
			HttpAccessLogStatsJmsListener httpAccessLogStatsJmsListener) {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(cachingConnectionFactory);
		container.setDestinationName(statsQueue);
		container.setMessageListener(httpAccessLogStatsJmsListener);
		return container;
	}
}
