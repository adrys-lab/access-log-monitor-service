package com.adrian.rebollo.service;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.test.util.ReflectionTestUtils;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.model.AlertType;
import com.adrian.rebollo.model.AccessLogAlert;
import com.adrian.rebollo.model.AccessLogStats;

@RunWith(MockitoJUnitRunner.class)
public class AccessLogAlertServiceImplTest {

	private AccessLogAlertServiceImpl httpAccessLogAlertService;

	@Mock
	private InternalDispatcher internalDispatcher;
	@Mock
	private StateMachine<AlertType, AlertType> stateMachine;
	@Mock
	private State<AlertType, AlertType> state;

	@Captor
	private ArgumentCaptor<AccessLogAlert> alertCaptor;

	@Before
	public void init() {

		// tests will be based on the assessment required time windows, stats delay and threshold, so 120 seconds for alerts time window and 10 seconds for report stats.
		// this affects directly how the threshold is evaluated.
		httpAccessLogAlertService = new AccessLogAlertServiceImpl(internalDispatcher, stateMachine);

		ReflectionTestUtils.setField(httpAccessLogAlertService, "alertTimeWindow", 120);
		ReflectionTestUtils.setField(httpAccessLogAlertService, "threshold", 10);
		ReflectionTestUtils.setField(httpAccessLogAlertService, "delayStats", 10000);

		httpAccessLogAlertService.init();

		when(stateMachine.getState()).thenReturn(state);
		when(state.getId()).thenReturn(AlertType.NO_ALERT);
	}

	@Test
	public void testEmptyResults() {

		ReflectionTestUtils.invokeMethod(httpAccessLogAlertService, "compute");

		verify(internalDispatcher).dispatch(alertCaptor.capture());

		Assert.assertEquals(AlertType.NO_ALERT, alertCaptor.getValue().getType());
	}

	@Test
	public void buildNoAlert() {

		httpAccessLogAlertService.handle(new AccessLogStats());

		verify(stateMachine).sendEvent(AlertType.NO_ALERT);
		verify(internalDispatcher).dispatch(alertCaptor.capture());

		Assert.assertEquals(AlertType.NO_ALERT, alertCaptor.getValue().getType());
	}

	@Test
	public void alertIsBuilt() {

		final AccessLogStats httpAccessLogLine = new AccessLogStats()
				.setRequests(new AtomicLong(3000));

		httpAccessLogAlertService.handle(httpAccessLogLine);

		verify(internalDispatcher).dispatch(alertCaptor.capture());
		verify(stateMachine).sendEvent(AlertType.HIGH_TRAFFIC);

		Assert.assertEquals(AlertType.HIGH_TRAFFIC, alertCaptor.getValue().getType());
		Assert.assertEquals(3000, alertCaptor.getValue().getRequests());
	}

	@Test
	public void alertTransitionsNoAlertTrafficRecoverNoAlert() {

		//////////////////FIRST LOG STATS/////////////////////////
		///***  This creates 12 log stats with 105 requests each = 1260 requests, in a timewindow of 120, gives a 10.5 requests per second, higher than threshold.
		IntStream.range(0, 12).forEach((i) -> httpAccessLogAlertService.handle(new AccessLogStats()
				.setRequests(new AtomicLong(105))));

		///*** 10.5 is higher than 10 threshold, so we expect HIGH TRAFFIC.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.HIGH_TRAFFIC);

		//////////////////SECOND LOG STATS/////////////////////////
		///***  This will add a new log stat, removing the oldest previous stats with 105 requests, and adding a new one with 10,
		//***   11 log stats with 105 requests each + 1 with 10 = 1165 requests, in a timewindow of 120, gives a 9.71 requests per second, lower than threshold.
		AccessLogStats httpAccessLogLine = new AccessLogStats()
				.setRequests(new AtomicLong(10));

		when(state.getId()).thenReturn(AlertType.HIGH_TRAFFIC);
		httpAccessLogAlertService.handle(httpAccessLogLine);

		///*** 9.71 is lower than 10 threshold, so we expect RECOVER cause previous state was HIGH TRAFFIC.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.RECOVER);

		//////////////////THIRD LOG STATS/////////////////////////
		///***  This will add a new log stat, removing the oldest previous stats with 105 requests, and adding a new one with 10,
		//***   10 log stats with 105 requests each + 2 with 10 = 1070 requests, in a timewindow of 120, gives a 8.92 requests per second, lower than threshold.
		httpAccessLogLine = new AccessLogStats()
				.setRequests(new AtomicLong(10));

		when(state.getId()).thenReturn(AlertType.RECOVER);
		httpAccessLogAlertService.handle(httpAccessLogLine);

		///*** 8.92 is lower than 10 threshold, so we expect NO_ALERT cause previous state was RECOVER.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.NO_ALERT);
	}

	@Test
	public void alertTransitionsNoAlertNoAlertTrafficRecoverTrafficRecoverNoAlert() {

		//////////////////FIRST LOG STATS/////////////////////////
		///***  This creates 12 log stats with 95 requests each = 1140 requests, in a timewindow of 120, gives a 9.5 requests per second, lower than threshold.
		IntStream.range(0, 12).forEach((i) -> httpAccessLogAlertService.handle(new AccessLogStats()
				.setRequests(new AtomicLong(95))));

		///*** 9.5 is not enough to trigger HIGH TRAFFIC, so we expect NO ALERT.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.NO_ALERT);

		//////////////////SECOND LOG STATS/////////////////////////
		///***  This will add a new log stat, removing the oldest previous stats with 95 requests, and adding a new one with 160,
		//***   11 log stats with 95 requests each + 1 with 160 = 1205 requests, in a timewindow of 120, gives a 10.04 requests per second, higher than threshold.
		AccessLogStats httpAccessLogLine = new AccessLogStats()
				.setRequests(new AtomicLong(160));

		httpAccessLogAlertService.handle(httpAccessLogLine);

		///*** 10.04 is enough to trigger HIGH TRAFFIC, so we expect NO TRAFFIC.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.HIGH_TRAFFIC);

		//////////////////THIRD LOG STATS/////////////////////////
		///***  This will add a new log stat, removing the oldest previous stats with 95 requests, and adding a new one with 85,
		//***   10 log stats with 95 requests each + 1 with 160 + 1 with 85 = 1195 requests, in a timewindow of 120, gives a 9.95 requests per second, lower than threshold.
		httpAccessLogLine = new AccessLogStats()
				.setRequests(new AtomicLong(85));

		when(state.getId()).thenReturn(AlertType.HIGH_TRAFFIC);
		httpAccessLogAlertService.handle(httpAccessLogLine);

		///*** 9.95 is lower than 10 threshold, so we expect RECOVER cause previous state was HIGH TRAFFIC.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.RECOVER);

		//////////////////FOURTH LOG STATS/////////////////////////
		///***  This will add a new log stat, removing the oldest previous stats with 95 requests, and adding a new one with 180,
		//***   9 log stats with 95 requests each + 1 with 160 + 1 with 85 + 1 with 180 = 1280 requests, in a timewindow of 120, gives a 10.66 requests per second, higher than threshold.
		httpAccessLogLine = new AccessLogStats()
				.setRequests(new AtomicLong(180));

		when(state.getId()).thenReturn(AlertType.RECOVER);
		httpAccessLogAlertService.handle(httpAccessLogLine);

		///*** 10.66 is higher than 10 threshold, so we expect HIGH_TRAFFIC.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.HIGH_TRAFFIC);

		//////////////////FIFTH LOG STATS/////////////////////////
		///***  This will add a new log stat, removing the oldest previous stats with 95 requests, and adding a new one with 75,
		//***   8 log stats with 95 requests each + 1 with 160 + 1 with 85 + 1 with 180 + 1 with 75 = 1195 requests, in a timewindow of 120, gives a 9.95 requests per second, lower than threshold.
		httpAccessLogLine = new AccessLogStats()
				.setRequests(new AtomicLong(10));

		when(state.getId()).thenReturn(AlertType.HIGH_TRAFFIC);
		httpAccessLogAlertService.handle(httpAccessLogLine);

		///*** 9.95 is lower than 10 threshold, so we expect RECOVER cause previous state was HIGH TRAFFIC.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.RECOVER);

		//////////////////SIXTH LOG STATS/////////////////////////
		///***  This will add a new log stat, removing the oldest previous stats with 95 requests, and adding a new one with 75,
		//***   7 log stats with 95 requests each + 1 with 160 + 1 with 85 + 1 with 180 + 1 with 75 + 1 with 90 = 1175 requests, in a timewindow of 120, gives a 9.79 requests per second, lower than threshold.
		httpAccessLogLine = new AccessLogStats()
				.setRequests(new AtomicLong(75));

		when(state.getId()).thenReturn(AlertType.RECOVER);
		httpAccessLogAlertService.handle(httpAccessLogLine);

		///*** 9.79 is lower than 10 threshold, so we expect NO_ALERT cause previous state was RECOVER.
		verify(stateMachine, atLeastOnce()).sendEvent(AlertType.NO_ALERT);
	}
}
