/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link TaskPersisterJpa}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskPersisterJpaTest {

	@Test
	public void changeTaskStatus() {
		TaskPersisterJpa tested = getTested();

		// case - unknown task
		Mockito.reset(tested.em);
		Mockito.when(tested.em.find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);
		Assert.assertFalse(tested.changeTaskStatus("myid", TaskStatus.RUNNING, "testmessage"));
		Mockito.verify(tested.em).find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE);

		// case - no any transition possible from final statuses
		for (TaskStatus actualStatus : new TaskStatus[] { TaskStatus.CANCELED, TaskStatus.FINISHED_ERROR,
				TaskStatus.FINISHED_OK }) {
			for (TaskStatus newStatus : TaskStatus.values()) {
				changeTaskstatusTestCaseImpl(tested, actualStatus, newStatus, false);
			}
		}

		// case - transitions from NEW
		{
			TaskStatus actualStatus = TaskStatus.NEW;
			for (TaskStatus newStatus : new TaskStatus[] { TaskStatus.NEW, TaskStatus.FAILOVER, TaskStatus.FINISHED_ERROR,
					TaskStatus.FINISHED_OK }) {
				changeTaskstatusTestCaseImpl(tested, actualStatus, newStatus, false);
			}
			for (TaskStatus newStatus : new TaskStatus[] { TaskStatus.RUNNING, TaskStatus.CANCELED }) {
				changeTaskstatusTestCaseImpl(tested, actualStatus, newStatus, true);
			}
		}

		// case - transitions from RUNNING
		{
			TaskStatus actualStatus = TaskStatus.RUNNING;
			for (TaskStatus newStatus : new TaskStatus[] { TaskStatus.NEW, TaskStatus.RUNNING }) {
				changeTaskstatusTestCaseImpl(tested, actualStatus, newStatus, false);
			}
			for (TaskStatus newStatus : new TaskStatus[] { TaskStatus.CANCELED, TaskStatus.FAILOVER,
					TaskStatus.FINISHED_ERROR, TaskStatus.FINISHED_OK }) {
				changeTaskstatusTestCaseImpl(tested, actualStatus, newStatus, true);
			}
		}

		// case - transitions from NEW
		{
			TaskStatus actualStatus = TaskStatus.FAILOVER;
			for (TaskStatus newStatus : new TaskStatus[] { TaskStatus.NEW, TaskStatus.FAILOVER, TaskStatus.FINISHED_ERROR,
					TaskStatus.FINISHED_OK }) {
				changeTaskstatusTestCaseImpl(tested, actualStatus, newStatus, false);
			}
			for (TaskStatus newStatus : new TaskStatus[] { TaskStatus.RUNNING, TaskStatus.CANCELED }) {
				changeTaskstatusTestCaseImpl(tested, actualStatus, newStatus, true);
			}
		}
	}

	private void changeTaskstatusTestCaseImpl(TaskPersisterJpa tested, TaskStatus actualStatus, TaskStatus newStatus,
			boolean retExpected) {
		Mockito.reset(tested.em);
		final TaskStatusInfo ret = createTaskStatusInfoWithStatus(actualStatus);
		Mockito.when(tested.em.find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE)).thenReturn(ret);
		Assert.assertEquals(retExpected, tested.changeTaskStatus("myid", newStatus, "testmessage"));
		Mockito.verify(tested.em).find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE);
		if (retExpected) {
			Assert.assertEquals("testmessage", ret.getProcessingLog());
			Assert.assertEquals(newStatus, ret.getTaskStatus());
			if (actualStatus == TaskStatus.RUNNING) {
				TestUtils.assertCurrentDate(ret.getLastRunFinishedAt());
			}
		} else {
			Assert.assertNull(ret.getProcessingLog());
			Assert.assertEquals(actualStatus, ret.getTaskStatus());
		}

	}

	@Test
	public void createTask() {
		TaskPersisterJpa tested = getTested();

		final String taskType = "tt";
		final Map<String, Object> taskConfig = new HashMap<String, Object>();

		Mockito.doAnswer(new Answer<Object>() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				TaskStatusInfo actual = (TaskStatusInfo) invocation.getArguments()[0];
				Assert.assertNotNull(actual.getId());
				TestUtils.assertCurrentDate(actual.getTaskCreatedAt());
				Assert.assertEquals(TaskStatus.NEW, actual.getTaskStatus());
				Assert.assertEquals(taskType, actual.getTaskType());
				TestUtils.assertJsonContent(taskConfig, actual.getTaskConfig());
				return null;
			}

		}).when(tested.em).persist(Mockito.any(TaskStatusInfo.class));

		Assert.assertNotNull(tested.createTask(taskType, taskConfig));
		Mockito.verify(tested.em).persist(Mockito.any(TaskStatusInfo.class));
	}

	@Test
	public void markTaskToBeCancelled() {
		TaskPersisterJpa tested = getTested();

		markTaskToBeCancelledImpl(tested, null, false);

		{
			Mockito.reset(tested.em);
			TaskStatusInfo tsi = createTaskStatusInfoWithStatus(TaskStatus.NEW);
			markTaskToBeCancelledImpl(tested, tsi, true);
			Assert.assertTrue(tsi.isCancelRequested());
		}

		// case - no duplicate set if set already
		{
			Mockito.reset(tested.em);
			TaskStatusInfo tsi = createTaskStatusInfoWithStatus(TaskStatus.NEW);
			tsi.setCancelRequested(true);
			markTaskToBeCancelledImpl(tested, tsi, false);
			Assert.assertTrue(tsi.isCancelRequested());
		}

		{
			Mockito.reset(tested.em);
			TaskStatusInfo tsi = createTaskStatusInfoWithStatus(TaskStatus.RUNNING);
			markTaskToBeCancelledImpl(tested, tsi, true);
			Assert.assertTrue(tsi.isCancelRequested());
		}

		{
			Mockito.reset(tested.em);
			TaskStatusInfo tsi = createTaskStatusInfoWithStatus(TaskStatus.FAILOVER);
			markTaskToBeCancelledImpl(tested, tsi, true);
			Assert.assertTrue(tsi.isCancelRequested());
		}

		{
			Mockito.reset(tested.em);
			TaskStatusInfo tsi = createTaskStatusInfoWithStatus(TaskStatus.CANCELED);
			markTaskToBeCancelledImpl(tested, tsi, false);
			Assert.assertFalse(tsi.isCancelRequested());
		}

		{
			Mockito.reset(tested.em);
			TaskStatusInfo tsi = createTaskStatusInfoWithStatus(TaskStatus.FINISHED_OK);
			markTaskToBeCancelledImpl(tested, tsi, false);
			Assert.assertFalse(tsi.isCancelRequested());
		}

		{
			Mockito.reset(tested.em);
			TaskStatusInfo tsi = createTaskStatusInfoWithStatus(TaskStatus.FINISHED_ERROR);
			markTaskToBeCancelledImpl(tested, tsi, false);
			Assert.assertFalse(tsi.isCancelRequested());
		}

	}

	private void markTaskToBeCancelledImpl(TaskPersisterJpa tested, TaskStatusInfo tsi, boolean expectedReturn) {
		Mockito.when(tested.em.find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE)).thenReturn(tsi);
		Assert.assertEquals(expectedReturn, tested.markTaskToBeCancelled("myid"));
		Mockito.verify(tested.em).find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE);

	}

	private TaskStatusInfo createTaskStatusInfoWithStatus(TaskStatus taskStatus) {
		TaskStatusInfo ret = new TaskStatusInfo();
		ret.setId("myid");
		ret.setTaskStatus(taskStatus);
		return ret;
	}

	@Test
	public void getTaskStatusInfo() {
		TaskPersisterJpa tested = getTested();

		{
			Mockito.when(tested.em.find(TaskStatusInfo.class, "myid")).thenReturn(null);
			Assert.assertNull(tested.getTaskStatusInfo("myid"));
			Mockito.verify(tested.em).find(TaskStatusInfo.class, "myid");
		}

		{
			Mockito.reset(tested.em);
			final TaskStatusInfo ret = createTaskStatusInfoWithStatus(TaskStatus.FAILOVER);
			Mockito.when(tested.em.find(TaskStatusInfo.class, "myid")).thenReturn(ret);
			Assert.assertEquals(ret, tested.getTaskStatusInfo("myid"));
			Mockito.verify(tested.em).find(TaskStatusInfo.class, "myid");
		}
	}

	@Test
	public void writeTaskLog() {
		TaskPersisterJpa tested = getTested();

		tested.writeTaskLog("myid", null);
		Mockito.verifyZeroInteractions(tested.em);

		tested.writeTaskLog("myid", "   ");
		Mockito.verifyZeroInteractions(tested.em);

		// case - unknown task
		Mockito.reset(tested.em);
		Mockito.when(tested.em.find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);
		tested.writeTaskLog("myid", "testmessage");
		Mockito.verify(tested.em).find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE);

		// case - known task
		{
			Mockito.reset(tested.em);
			final TaskStatusInfo ret = createTaskStatusInfoWithStatus(TaskStatus.FAILOVER);
			Mockito.when(tested.em.find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE)).thenReturn(ret);
			tested.writeTaskLog("myid", "testmessage");
			Mockito.verify(tested.em).find(TaskStatusInfo.class, "myid", LockModeType.PESSIMISTIC_WRITE);
			Assert.assertEquals("testmessage", ret.getProcessingLog());
		}

	}

	private TaskPersisterJpa getTested() {
		TaskPersisterJpa t = new TaskPersisterJpa();
		t.em = Mockito.mock(EntityManager.class);
		return t;
	}

}
