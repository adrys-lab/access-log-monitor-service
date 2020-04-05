package com.adrian.rebollo;

import java.util.Arrays;
import java.util.HashSet;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import com.adrian.rebollo.model.AlertType;

/**
 * Spring convention for State Machine Pattern.
 * Will be used to control the Alerts transitions flow, events and lifecycle.
 */
@Configuration
@EnableStateMachine
class AlertStateMachineConfiguration extends StateMachineConfigurerAdapter<AlertType, AlertType> {

	/**
	 * Initial Machine Stat Alert is NO ALERT.
	 */
	@Override
	public void configure(StateMachineStateConfigurer<AlertType, AlertType> states) throws Exception {
		states.withStates()
				.initial(AlertType.NO_ALERT)
				.end(AlertType.NO_ALERT)
				.states(new HashSet<>(Arrays.asList(AlertType.values())));
	}

	/**
	 * This configures the TreeMap of the available States Graph.
	 * Transitions could be:
	 * NO_ALERT -> NO_ALERT
	 * NO_ALERT -> HIGH_TRAFFIC
	 * HIGH_TRAFFIC -> HIGH_TRAFFIC
	 * HIGH_TRAFFIC -> RECOVER
	 * RECOVER -> HIGH_TRAFFIC
	 * RECOVER -> NO_ALERT
	 */
	@Override
	public void configure( StateMachineTransitionConfigurer<AlertType, AlertType> transitions) throws Exception {

		transitions.withExternal()
				.source(AlertType.NO_ALERT).target(AlertType.NO_ALERT).event(AlertType.NO_ALERT).and()
				.withExternal().source(AlertType.NO_ALERT).target(AlertType.HIGH_TRAFFIC).event(AlertType.HIGH_TRAFFIC).and()
				.withExternal()
				.source(AlertType.HIGH_TRAFFIC).target(AlertType.HIGH_TRAFFIC).event(AlertType.HIGH_TRAFFIC).and()
				.withExternal()
				.source(AlertType.HIGH_TRAFFIC).target(AlertType.RECOVER).event(AlertType.RECOVER).and()
				.withExternal()
				.source(AlertType.RECOVER).target(AlertType.HIGH_TRAFFIC).event(AlertType.HIGH_TRAFFIC).and()
				.withExternal()
				.source(AlertType.RECOVER).target(AlertType.NO_ALERT).event(AlertType.NO_ALERT);
	}
}
