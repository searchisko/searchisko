/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.service.TaskService;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.TaskManager;
import org.searchisko.api.tasker.TaskStatus;
import org.searchisko.api.tasker.TaskStatusInfo;
import org.searchisko.api.tasker.UnsupportedTaskException;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link TaskRestService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskRestServiceTest {

	@Test
	public void getTypes() {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);

		List<String> value = new ArrayList<String>();
		Mockito.when(managerMock.listSupportedTaskTypes()).thenReturn(value);
		Assert.assertEquals(value, tested.getTypes());
		Mockito.verify(managerMock).listSupportedTaskTypes();
	}

	@Test(expected = RuntimeException.class)
	public void getTypes_errorFromSerice() {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);
		Mockito.reset(managerMock);
		Mockito.when(managerMock.listSupportedTaskTypes()).thenThrow(new RuntimeException("test exception"));
		TestUtils.assertResponseStatus(tested.getTypes(), Status.INTERNAL_SERVER_ERROR);
	}

	@Test
	public void getTasks() {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);

		// case - ok no params
		{
			List<TaskStatusInfo> value = new ArrayList<TaskStatusInfo>();
			Mockito.when(managerMock.listTasks(null, null, 0, 0)).thenReturn(value);
			Assert.assertEquals(value, tested.getTasks(null, null, null, null));
			Mockito.verify(managerMock).listTasks(null, null, 0, 0);
		}

		// case - params passing
		{
			Mockito.reset(managerMock);
			List<TaskStatusInfo> value = new ArrayList<TaskStatusInfo>();
			String[] ts = new String[] { TaskStatus.FAILOVER.toString() };
			List<TaskStatus> tslist = new ArrayList<TaskStatus>();
			tslist.add(TaskStatus.FAILOVER);
			Mockito.when(managerMock.listTasks("aa", tslist, 10, 5)).thenReturn(value);
			Assert.assertEquals(value, tested.getTasks("aa", ts, 10, 5));
			Mockito.verify(managerMock).listTasks("aa", tslist, 10, 5);
		}

	}

	@Test(expected = RuntimeException.class)
	public void getTasks_errorFromSerice() {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);
		Mockito.reset(managerMock);
		Mockito.when(managerMock.listTasks(null, null, 0, 0)).thenThrow(new RuntimeException("test exception"));
		TestUtils.assertResponseStatus(tested.getTasks(null, null, 0, 0), Status.INTERNAL_SERVER_ERROR);

	}

	@Test
	public void getTask() {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);

		// case - found
		{
			TaskStatusInfo value = new TaskStatusInfo();
			Mockito.when(managerMock.getTaskStatusInfo("myid")).thenReturn(value);
			Assert.assertEquals(value, tested.getTask("myid"));
			Mockito.verify(managerMock).getTaskStatusInfo("myid");
		}

		// case - not found
		{
			Mockito.reset(managerMock);
			Mockito.when(managerMock.getTaskStatusInfo("myid")).thenReturn(null);
			TestUtils.assertResponseStatus(tested.getTask("myid"), Status.NOT_FOUND);
			Mockito.verify(managerMock).getTaskStatusInfo("myid");
		}

	}

	@Test(expected = RuntimeException.class)
	public void getTask_erorrFromSerice() {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);

		Mockito.reset(managerMock);
		Mockito.when(managerMock.getTaskStatusInfo("myid")).thenThrow(new RuntimeException("test exception"));
		TestUtils.assertResponseStatus(tested.getTask("myid"), Status.INTERNAL_SERVER_ERROR);

	}

	@SuppressWarnings("unchecked")
	@Test
	public void createTask() throws UnsupportedTaskException, TaskConfigurationException {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);

		// case - ok
		{
			String value = "idsd";
			Map<String, Object> cfg = new HashMap<String, Object>();
			Mockito.when(managerMock.createTask("mytype", cfg)).thenReturn(value);
			Map<String, Object> result = (Map<String, Object>) tested.createTask("mytype", cfg);
			Assert.assertNotNull(result);
			Assert.assertEquals(value, result.get("id"));
			Mockito.verify(managerMock).createTask("mytype", cfg);
		}

		// case - UnsupportedTaskException
		{
			Mockito.reset(managerMock);
			Mockito.when(managerMock.createTask("mytype", null)).thenThrow(new UnsupportedTaskException("test exception"));
			TestUtils.assertResponseStatus(tested.createTask("mytype", null), Status.BAD_REQUEST);
		}

		// case - TaskConfigurationException
		{
			Mockito.reset(managerMock);
			Mockito.when(managerMock.createTask("mytype", null)).thenThrow(new TaskConfigurationException("test exception"));
			TestUtils.assertResponseStatus(tested.createTask("mytype", null), Status.BAD_REQUEST);
		}

	}

	@Test(expected = RuntimeException.class)
	public void createTask_errorFromSerice() throws UnsupportedTaskException, TaskConfigurationException {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);
		Mockito.reset(managerMock);
		Mockito.when(managerMock.createTask("mytype", null)).thenThrow(new RuntimeException("test exception"));
		TestUtils.assertResponseStatus(tested.createTask("mytype", null), Status.INTERNAL_SERVER_ERROR);

	}

	@Test
	public void cancelTask() {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);

		// case - ok
		{
			Mockito.reset(managerMock);
			Mockito.when(managerMock.cancelTask("myid")).thenReturn(true);
			TestUtils.assertResponseStatus(tested.cancelTask("myid"), Status.OK, "Task canceled");
		}
		{
			Mockito.reset(managerMock);
			Mockito.when(managerMock.cancelTask("myid")).thenReturn(false);
			TestUtils.assertResponseStatus(tested.cancelTask("myid"), Status.OK, "Task not canceled");
		}

	}

	@Test(expected = RuntimeException.class)
	public void cancelTask_errorFromService() {
		TaskRestService tested = getTested();
		TaskManager managerMock = mockTaskManager(tested);

		Mockito.reset(managerMock);
		Mockito.when(managerMock.cancelTask("myid")).thenThrow(new RuntimeException("test exception"));
		TestUtils.assertResponseStatus(tested.cancelTask("myid"), Status.INTERNAL_SERVER_ERROR);

	}

	private TaskRestService getTested() {
		TaskRestService ret = new TaskRestService();
		ret.log = Logger.getLogger("testlogger");
		return ret;
	}

	private TaskManager mockTaskManager(TaskRestService tested) {
		tested.taskService = Mockito.mock(TaskService.class);
		TaskManager value = Mockito.mock(TaskManager.class);
		Mockito.when(tested.taskService.getTaskManager()).thenReturn(value);
		return value;
	}

}
