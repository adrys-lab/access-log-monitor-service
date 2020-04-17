package com.adrian.rebollo.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import com.adrian.rebollo.api.AccessLogAlertService;
import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.AlertType;
import com.adrian.rebollo.model.AccessLogAlert;
import com.adrian.rebollo.model.AccessLogStats;
import com.google.common.collect.EvictingQueue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessLogAlertServiceImpl implements AccessLogAlertService {

	private final InternalDispatcher internalDispatcher;

	private Queue<AccessLogStats> statsQueue;

	@Value("${service.alert.time-window}")
	private int alertTimeWindow;
	@Value("${service.schedulers.stats.delay}")
	private int delayStats;
	@Value("${service.alert.threshold}")
	private int threshold;

	@Autowired
	private final StateMachine<AlertType, AlertType> alertStateMachine;

	@PostConstruct
	public void init() {
		//Starts the Alert State Machine
		alertStateMachine.start();
		statsQueue = EvictingQueue.create(alertTimeWindow / (delayStats / 1000));
	}

	public void handle(AccessLogStats accessLogStats) {
		statsQueue.offer(accessLogStats);
		compute();
	}

	//compute the stats to build the alert
	private void compute() {

		final LocalDateTime end = LocalDateTime.now();
		final LocalDateTime start = end.minus(alertTimeWindow, ChronoUnit.SECONDS);

		LOG.info("Triggered alert compute logs statistics for a timewindow={} SECONDS, from={}, to={}.", alertTimeWindow, start, end);

		final List<AccessLogStats> stats = new ArrayList<>(statsQueue);

		LOG.info("Proceeding to check Alerts from {} stats data.", stats.size());

		final long totalRequests = stats.stream()
				.map(AccessLogStats::getRequests)
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
	}

	private void createAlert(long totalRequests, double requestsSecond, final AlertType alertType, final LocalDateTime start, final LocalDateTime end) {

		final AccessLogAlert accessLogAlert = AccessLogAlert.builder()
				.alertTime(LocalDateTime.now())
				.start(start)
				.end(end)
				.requests(totalRequests)
				.requestsSecond(requestsSecond)
				.type(alertType)
				.build();

		LOG.info("Proceeding to create alert={}.", accessLogAlert);

		//here we update the alert state machine with the given alert type.
		alertStateMachine.sendEvent(accessLogAlert.getType());

		//alaways send the alert to be dispatched. it has to reach the LogService anyways.
		internalDispatcher.dispatch(accessLogAlert);
	}
}
