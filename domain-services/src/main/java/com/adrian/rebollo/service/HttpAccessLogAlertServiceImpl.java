package com.adrian.rebollo.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.dao.HttpAccessLogStatsDao;
import com.adrian.rebollo.model.AlertType;
import com.adrian.rebollo.model.HttpAccessLogAlert;
import com.adrian.rebollo.model.HttpAccessLogStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpAccessLogAlertServiceImpl {

	private final HttpAccessLogStatsDao httpAccessLogStatsDao;
	private final InternalDispatcher internalDispatcher;

	@Value("${service.schedulers.alert.time-window}")
	private int alertTimeWindow;
	@Value("${service.schedulers.alert.threshold}")
	private int threshold;
	@Value("${service.schedulers.alert.chunk}")
	private int chunk;

	@Autowired
	private final StateMachine<AlertType, AlertType> alertStateMachine;

	@PostConstruct
	public void init() {
		//Starts the Alert State Machine
		alertStateMachine.start();
	}

	/**
	 * This scheduler triggers Log Alerts creation.
	 * Triggered each (default) 10 seconds.
	 * the window alert is default 120sec, and is the one that determines which dates to query and the average threshold evaluation.
	 * It has an initial delay higher (1sec higher) than {@link com.adrian.rebollo.service.HttpAccessLogStatsServiceImpl} to not make them clash.
	 * So we ensure each triggers differs in 1sec.
	 * The method gets the last 120sec added stats, compute them, and dispatch through internal Queue Message Broker.
	 */
	@ConditionalOnProperty( "service.schedulers.alert.enabled" )
	@Scheduled(fixedDelayString = "${service.schedulers.alert.delay}", initialDelayString = "${service.schedulers.alert.initial-delay}")
	public void alert() {

		final LocalDateTime end = LocalDateTime.now();
		final LocalDateTime start = end.minus(alertTimeWindow, ChronoUnit.SECONDS);

		/*
		 * Notice this triggers the last 10sec added Log Lines.
		 * ie:
		 * 1st - from x10 to x130
		 * 2nd - from x20 to x140
		 * 3rd - from x30 to x150
		 * ...
		 */
		LOG.info("Triggered alert compute logs statistics for a timewindow={} SECONDS, from={}, to={}.", alertTimeWindow, start, end);

		final Optional<List<HttpAccessLogStats>> optionalStats = httpAccessLogStatsDao.findByDateBetween(start, end, chunk);

		if(optionalStats.isPresent() && !optionalStats.get().isEmpty()) {
			final List<HttpAccessLogStats> stats = optionalStats.get();

			LOG.info("Proceeding to check Alerts from {} stats data.", stats.size());

			final long totalRequests = stats.stream()
					.map(HttpAccessLogStats::getRequests)
					.map(AtomicLong::get)
					.mapToLong(Long::longValue)
					.sum();

			//This gives the total requests per second (average) that will alow to contrast versus the threshold.
			double requestsSecond = Math.round((float) totalRequests / alertTimeWindow* 100.0) / 100.0;

			LOG.info("Computed Alert totalRequests={}, requestsSecond={}.", totalRequests, requestsSecond);

			if(requestsSecond >= threshold) {
				createAlert(totalRequests, requestsSecond, AlertType.HIGH_TRAFFIC, start, end);
			} else if(alertStateMachine.getState().getId() == AlertType.HIGH_TRAFFIC && requestsSecond < threshold) {
				createAlert(totalRequests, requestsSecond, AlertType.RECOVER, start, end);
			} else if(alertStateMachine.getState().getId() == AlertType.RECOVER || alertStateMachine.getState().getId() == AlertType.NO_ALERT) {
				createAlert(totalRequests, requestsSecond, AlertType.NO_ALERT, start, end);
			}
		} else {
			if(alertStateMachine.getState().getId() == AlertType.HIGH_TRAFFIC) {
				createAlert(0, 0, AlertType.RECOVER, start, end);
			} else if(alertStateMachine.getState().getId() == AlertType.RECOVER || alertStateMachine.getState().getId() == AlertType.NO_ALERT) {
				createAlert(0, 0, AlertType.NO_ALERT, start, end);
			}
		}
	}

	private void createAlert(long totalRequests, double requestsSecond, final AlertType alertType, final LocalDateTime start, final LocalDateTime end) {

		final HttpAccessLogAlert httpAccessLogAlert = HttpAccessLogAlert.builder()
				.alertTime(LocalDateTime.now())
				.start(start)
				.end(end)
				.requests(totalRequests)
				.requestsSecond(requestsSecond)
				.type(alertType)
				.build();

		LOG.info("Proceeding to create alert={}.", httpAccessLogAlert);

		//here we update the alert state machine with the given alert type.
		alertStateMachine.sendEvent(httpAccessLogAlert.getType());

		//alaways send the alert to be dispatched. it has to reach the LogService anyways.
		internalDispatcher.dispatch(httpAccessLogAlert);
	}
}
