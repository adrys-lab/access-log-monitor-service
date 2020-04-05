package com.adrian.rebollo.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.test.util.ReflectionTestUtils;

import com.adrian.rebollo.api.InternalDispatcher;
import com.adrian.rebollo.dao.HttpAccessLogStatsDao;
import com.adrian.rebollo.model.AlertType;
import com.adrian.rebollo.model.HttpAccessLogAlert;
import com.adrian.rebollo.model.HttpAccessLogStats;

@RunWith(MockitoJUnitRunner.class)
public class HttpAccessLogAlertServiceImplTest {

	@InjectMocks
	private HttpAccessLogAlertServiceImpl httpAccessLogAlertService;

	@Mock
	private HttpAccessLogStatsDao httpAccessLogStatsDao;
	@Mock
	private InternalDispatcher internalDispatcher;
	@Mock
	private StateMachine<AlertType, AlertType> stateMachine;
	@Mock
	private State<AlertType, AlertType> state;

	@Captor
	private ArgumentCaptor<HttpAccessLogAlert> alertCaptor;

	@Before
	public void init() {
		ReflectionTestUtils.setField(httpAccessLogAlertService, "alertTimeWindow", 120);
		ReflectionTestUtils.setField(httpAccessLogAlertService, "threshold", 10);
		ReflectionTestUtils.setField(httpAccessLogAlertService, "chunk", 10);

		when(stateMachine.getState()).thenReturn(state);
		when(state.getId()).thenReturn(AlertType.NO_ALERT);
	}

	@Test
	public void testEmptyResults() {

		httpAccessLogAlertService.alert();

		verify(httpAccessLogStatsDao).findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10));

		verify(internalDispatcher).dispatch(alertCaptor.capture());

		Assert.assertEquals(AlertType.NO_ALERT, alertCaptor.getValue().getType());
	}

	@Test
	public void buildNoAlert() {

		final HttpAccessLogStats httpAccessLogLine = new HttpAccessLogStats();

		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		verify(stateMachine).sendEvent(AlertType.NO_ALERT);
		verify(internalDispatcher).dispatch(alertCaptor.capture());

		Assert.assertEquals(AlertType.NO_ALERT, alertCaptor.getValue().getType());
	}

	@Test
	public void alertIsBuilt() {

		final HttpAccessLogStats httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(3000));

		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		verify(internalDispatcher).dispatch(alertCaptor.capture());
		verify(stateMachine).sendEvent(AlertType.HIGH_TRAFFIC);

		Assert.assertEquals(AlertType.HIGH_TRAFFIC, alertCaptor.getValue().getType());
		Assert.assertEquals(3000, alertCaptor.getValue().getRequests());
	}

	@Test
	public void alertTransitionsNoAlertTrafficRecoverNoAlert() {

		//////////////////FIRST LOG STATS/////////////////////////
		HttpAccessLogStats httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(3000));

		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		verify(stateMachine).sendEvent(AlertType.HIGH_TRAFFIC);

		//////////////////SECOND LOG STATS/////////////////////////
		httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(200));

		when(state.getId()).thenReturn(AlertType.HIGH_TRAFFIC);
		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		verify(stateMachine).sendEvent(AlertType.RECOVER);

		//////////////////THIRD LOG STATS/////////////////////////
		httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(200));

		when(state.getId()).thenReturn(AlertType.RECOVER);
		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		verify(stateMachine).sendEvent(AlertType.NO_ALERT);
	}

	@Test
	public void alertTransitionsNoAlertNoAlertTrafficRecoverTrafficRecoverNoAlert() {

		//////////////////FIRST LOG STATS/////////////////////////
		HttpAccessLogStats httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(1000));

		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		//////////////////1000 REQUESTS IS NOT ENOUGH TO TRIGGER HIGH TRAFFIC --> NO_ALERT /////////////////////////
		verify(stateMachine).sendEvent(AlertType.NO_ALERT);

		//////////////////SECOND LOG STATS/////////////////////////
		httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(2500));

		when(state.getId()).thenReturn(AlertType.NO_ALERT);
		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		//////////////////2500 REQUESTS IS ENOUGH TO TRIGGER HIGH TRAFFIC --> HIGH_TRAFFIC /////////////////////////
		verify(stateMachine).sendEvent(AlertType.HIGH_TRAFFIC);

		//////////////////THIRD LOG STATS/////////////////////////
		httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(500));

		when(state.getId()).thenReturn(AlertType.HIGH_TRAFFIC);
		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		//////////////////500 REQUESTS IS ENOUGH TO TRIGGER HIGH TRAFFIC, SO SWITCH STATE FROM HIGH_TRAFFIC --> RECOVER /////////////////////////
		verify(stateMachine).sendEvent(AlertType.RECOVER);

		//////////////////FOURTH LOG STATS/////////////////////////
		httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(5200));

		when(state.getId()).thenReturn(AlertType.RECOVER);
		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		//////////////////5200 REQUESTS IS ENOUGH TO TRIGGER HIGH TRAFFIC, SO SWITCH STATE FROM RECOVER --> HIGH_TRAFFIC /////////////////////////
		httpAccessLogAlertService.alert();

		verify(stateMachine, Mockito.times(2)).sendEvent(AlertType.HIGH_TRAFFIC);

		//////////////////FIFTH LOG STATS/////////////////////////
		httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(200));

		when(state.getId()).thenReturn(AlertType.HIGH_TRAFFIC);
		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		//////////////////200 REQUESTS IS ENOUGH TO TRIGGER HIGH TRAFFIC, SO SWITCH STATE FROM HIGH_TRAFFIC --> RECOVER /////////////////////////
		httpAccessLogAlertService.alert();

		verify(stateMachine, Mockito.times(2)).sendEvent(AlertType.RECOVER);

		//////////////////SIXTH LOG STATS/////////////////////////
		httpAccessLogLine = new HttpAccessLogStats()
				.setRequests(new AtomicLong(200));

		when(state.getId()).thenReturn(AlertType.RECOVER);
		when(httpAccessLogStatsDao.findByDateBetween(any(LocalDateTime.class), any(LocalDateTime.class), eq(10))).thenReturn(Optional.of(List.of(httpAccessLogLine)));

		httpAccessLogAlertService.alert();

		//////////////////200 REQUESTS IS ENOUGH TO TRIGGER HIGH TRAFFIC, SO SWITCH STATE FROM RECOVER --> NO_ALERT /////////////////////////
		verify(stateMachine, Mockito.times(2)).sendEvent(AlertType.NO_ALERT);
	}
}
