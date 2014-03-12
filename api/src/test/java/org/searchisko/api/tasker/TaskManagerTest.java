/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link TaskManager}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskManagerTest {

	@Test
	public void cancelTask() {
		TaskManager tested = getTested();
		Mockito.when(tested.taskPersister.markTaskToBeCancelled("1")).thenReturn(true);
		Mockito.when(tested.taskPersister.markTaskToBeCancelled("2")).thenReturn(false);

		tested.cancelTask("1");
		Mockito.verify(tested.taskPersister).markTaskToBeCancelled("1");
		Mockito.verify(tested.taskRunner).cancelTask("1");

		tested.cancelTask("2");
		Mockito.verify(tested.taskPersister).markTaskToBeCancelled("1");
		Mockito.verifyNoMoreInteractions(tested.taskRunner);

		// no NPE if runner do not exists
		tested.taskRunner = null;
		Mockito.verify(tested.taskPersister, Mockito.times(1)).markTaskToBeCancelled("1");

	}

	@Test
	public void startStopTasksExecution() throws InterruptedException {
		TaskManager tested = getTested();

		tested.taskRunner = null;
		Assert.assertNull(tested.taskRunner);
		// case - stop no effect when not running
		tested.stopTasksExecution();

		// case - start it
		tested.startTasksExecution();
		Assert.assertNotNull(tested.taskRunner);
		TaskRunner tr = tested.taskRunner;
		Thread.sleep(200);
		Assert.assertTrue(tr.isAlive());

		// case - second start has no effect
		tested.startTasksExecution();
		Assert.assertEquals(tr, tested.taskRunner);

		// case - stop it
		tested.stopTasksExecution();
		Assert.assertNull(tested.taskRunner);
		Thread.sleep(200);
		synchronized (tr) {
			Assert.assertTrue(tr.isInterrupted() || !tr.isAlive());
		}
	}

	@Test
	public void createTask() throws UnsupportedTaskException, TaskConfigurationException {
		TaskManager tested = getTested();
		Map<String, Object> taskConfig = new HashMap<String, Object>();

		// case - all ok
		Mockito.when(tested.taskFactory.createTask("tasktype", taskConfig)).thenReturn(Mockito.mock(Task.class));
		Mockito.when(tested.taskPersister.createTask("tasktype", taskConfig)).thenReturn("myid");
		Assert.assertEquals("myid", tested.createTask("tasktype", taskConfig));
		Mockito.verify(tested.taskRunner).notifyNewTaskAvailableForRun();

		// case - exception propagation
		Mockito.reset(tested.taskFactory, tested.taskPersister, tested.taskRunner);
		Mockito.when(tested.taskFactory.createTask("tasktype", taskConfig)).thenThrow(new UnsupportedTaskException(""));
		try {
			tested.createTask("tasktype", taskConfig);
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {

		}
		Mockito.verifyZeroInteractions(tested.taskPersister, tested.taskRunner);

	}

	private TaskManager getTested() {
		TaskManager ret = new TaskManager("mynode", Mockito.mock(TaskFactory.class), Mockito.mock(TaskPersister.class));
		ret.taskRunner = Mockito.mock(TaskRunner.class);
		return ret;
	}
}
