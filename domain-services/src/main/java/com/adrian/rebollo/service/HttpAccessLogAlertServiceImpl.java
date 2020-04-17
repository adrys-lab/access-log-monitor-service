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

import com.adrian.rebollo.api.HttpAccessLogAlertService;
import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.AlertType;
import com.adrian.rebollo.model.HttpAccessLogAlert;
import com.adrian.rebollo.model.HttpAccessLogStats;
import com.google.common.collect.EvictingQueue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpAccessLogAlertServiceImpl implements HttpAccessLogAlertService {

	private final InternalDispatcher internalDispatcher;

	private Queue<HttpAccessLogStats> statsQueue;

	@Value("${service.schedulers.alert.time-window}")
	private final int alertTimeWindow;
	@Value("${service.schedulers.stats.delay}")
	private final int delayStats;
	@Value("${service.schedulers.alert.threshold}")
	private final int threshold;

	@Autowired
	private final StateMachine<AlertType, AlertType> alertStateMachine;

	@PostConstruct
	public void init() {
		//Starts the Alert State Machine
		alertStateMachine.start();
		statsQueue = EvictingQueue.create(alertTimeWindow / (delayStats / 1000));
	}

	public void handle(HttpAccessLogStats httpAccessLogStats) {
		statsQueue.offer(httpAccessLogStats);
		compute();
	}

	//compute the stats to build the alert
	private void compute() {

		final LocalDateTime end = LocalDateTime.now();
		final LocalDateTime start = end.minus(alertTimeWindow, ChronoUnit.SECONDS);

		LOG.info("Triggered alert compute logs statistics for a timewindow={} SECONDS, from={}, to={}.", alertTimeWindow, start, end);

		final List<HttpAccessLogStats> stats = new ArrayList<>(statsQueue);

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
