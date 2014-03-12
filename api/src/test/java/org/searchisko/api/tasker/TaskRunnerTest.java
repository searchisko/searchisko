/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.lang.Thread.State;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link TaskRunner}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskRunnerTest {

	private static final String TASK_TYPE_TEST = "taskTypeTest";
	private static final String TESTNODEID = "testnodeid";

	@Test
	public void constructor() {
		TaskFactory taskFactory = Mockito.mock(TaskFactory.class);
		TaskPersister taskPersister = Mockito.mock(TaskPersister.class);
		TaskRunner tested = new TaskRunner("ndid", taskFactory, taskPersister);
		Assert.assertEquals("ndid", tested.nodeId);
		Assert.assertEquals(taskFactory, tested.taskFactory);
		Assert.assertEquals(taskPersister, tested.taskPersister);
		// case - assert last heartbeat initialization so no HB runs after start immediately
		TestUtils.assertCurrentDate(tested.lastHb);
	}

	@Test
	public void handleCancelRequests() throws InterruptedException {
		TaskRunner tested = getTested();

		Task t1 = createTaskMock(true);
		Task t3 = createTaskMock(true);
		Task t5 = createTaskMock(true);
		Task t6 = createTaskMock(true);
		t6.setCanceled(true);

		tested.runningTasks.put("t1", t1);
		tested.runningTasks.put("t2", createTaskMock(false));
		tested.runningTasks.put("t3", t3);
		tested.runningTasks.put("t4", createTaskMock(false));
		tested.runningTasks.put("t5", t5);
		tested.runningTasks.put("t6", t6);

		Thread.sleep(1000);

		Mockito.when(tested.taskPersister.getTaskStatusInfo("t1")).thenReturn(createTaskStatusInfoCancelTest(false));
		Mockito.when(tested.taskPersister.getTaskStatusInfo("t3")).thenReturn(createTaskStatusInfoCancelTest(true));
		Mockito.when(tested.taskPersister.getTaskStatusInfo("t5")).thenReturn(null);

		tested.handleCancelRequests();

		Assert.assertEquals(false, t1.isCanceledOrInterrupted());
		Assert.assertEquals(true, t3.isCanceledOrInterrupted());
		Assert.assertEquals(false, t5.isCanceledOrInterrupted());
		Assert.assertEquals(true, t6.isCanceledOrInterrupted());

		Mockito.verify(tested.taskPersister).getTaskStatusInfo("t1");
		Mockito.verify(tested.taskPersister).getTaskStatusInfo("t3");
		Mockito.verify(tested.taskPersister).getTaskStatusInfo("t5");
		Mockito.verifyNoMoreInteractions(tested.taskPersister);
	}

	private TaskStatusInfo createTaskStatusInfoCancelTest(boolean cancelRequsted) {
		TaskStatusInfo ret = new TaskStatusInfo();
		ret.cancelRequested = cancelRequsted;
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void startTasks() throws UnsupportedTaskException, TaskConfigurationException {
		TaskRunner tested = getTested();

		// case - nothing to run in persister
		Mockito.when(tested.taskPersister.getTaskToRun(TESTNODEID)).thenReturn(null);
		tested.startTasks();
		Mockito.verify(tested.taskPersister).getTaskToRun(TESTNODEID);
		Mockito.verifyNoMoreInteractions(tested.taskPersister);
		Mockito.verifyNoMoreInteractions(tested.taskFactory);

		// case - start two new tasks
		Mockito.reset(tested.taskFactory, tested.taskPersister);
		Mockito.when(tested.taskPersister.getTaskToRun(TESTNODEID)).thenAnswer(new Answer<TaskStatusInfo>() {

			@Override
			public TaskStatusInfo answer(InvocationOnMock invocation) throws Throwable {
				return createTaskStatusInfoStartTest();
			}

		});

		Mockito.when(tested.taskFactory.createTask(Mockito.eq(TASK_TYPE_TEST), Mockito.anyMap())).thenAnswer(
				new Answer<Task>() {

					@Override
					public Task answer(InvocationOnMock invocation) throws Throwable {
						return createTaskMock();
					}
				});

		tested.startTasks();

		Assert.assertEquals(2, tested.runningTasks.size());
		for (Task task : tested.runningTasks.values()) {
			Assert.assertNotNull(task.context);
			Assert.assertNotNull(task.taskId);
			Assert.assertTrue(task.getState() != State.NEW);
		}
		Mockito.verify(tested.taskPersister, Mockito.times(2)).getTaskToRun(TESTNODEID);
		Mockito.verify(tested.taskFactory, Mockito.times(2)).createTask(Mockito.eq(TASK_TYPE_TEST), Mockito.anyMap());

		// case - all running slot full so nothing started
		Mockito.reset(tested.taskFactory, tested.taskPersister);
		tested.startTasks();
		Assert.assertEquals(2, tested.runningTasks.size());
		Mockito.verifyZeroInteractions(tested.taskPersister);
		Mockito.verifyZeroInteractions(tested.taskFactory);

		// case - exception during start is not thworn out of method
		Mockito.reset(tested.taskFactory, tested.taskPersister);
		Mockito.when(tested.taskPersister.getTaskToRun(TESTNODEID)).thenThrow(new RuntimeException("test exception"));
		tested.startTasks();
		Mockito.verifyZeroInteractions(tested.taskFactory);

	}

	int ctc = 1;

	private TaskStatusInfo createTaskStatusInfoStartTest() {
		TaskStatusInfo ti = new TaskStatusInfo();
		ti.id = "t" + ctc;
		ctc++;
		ti.taskType = TASK_TYPE_TEST;
		return ti;
	}

	private Task createTaskMock() {
		Task t = new Task() {
			@Override
			protected void performTask() throws Exception {
			}

		};
		t.setDaemon(true);
		return t;
	}

	@Test
	public void interruptRunningTasks() throws InterruptedException {
		TaskRunner tested = getTested();
		tested.runningTasks.put("t1", createTaskMock(true));
		tested.runningTasks.put("t2", createTaskMock(false));
		tested.runningTasks.put("t3", createTaskMock(true));
		tested.runningTasks.put("t4", createTaskMock(false));
		tested.runningTasks.put("t5", createTaskMock(true));
		tested.runningTasks.put("t6", createTaskMock(true));

		Thread.sleep(200);
		tested.interruptRunningTasks();
		for (Task task : tested.runningTasks.values()) {
			Assert.assertTrue(!task.isAlive() || task.isInterrupted());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void heartbeat() {
		TaskRunner tested = getTested();

		// case - no heartbeat performed
		{
			long lhb = System.currentTimeMillis() - tested.hbPeriod + 200;
			tested.lastHb = lhb;

			tested.heartbeat();
			Assert.assertEquals(lhb, tested.lastHb);
			Mockito.verifyZeroInteractions(tested.taskPersister);
		}

		// case - heartbeat performed, no any running tasks in runner
		{
			Mockito.reset(tested.taskPersister);
			long lhb = System.currentTimeMillis() - tested.hbPeriod - 200;
			tested.lastHb = lhb;
			Mockito.doAnswer(new Answer() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Set<String> s = (Set<String>) invocation.getArguments()[1];
					Assert.assertNotNull(s);
					Assert.assertTrue(s.isEmpty());
					return null;
				}
			}).when(tested.taskPersister).heartbeat(Mockito.eq(TESTNODEID), Mockito.anySet(), Mockito.anyLong());

			tested.heartbeat();

			TestUtils.assertCurrentDate(tested.lastHb);
			Mockito.verify(tested.taskPersister).heartbeat(Mockito.eq(TESTNODEID), Mockito.anySet(),
					Mockito.eq(tested.hbPeriod * 5));
			Mockito.verifyNoMoreInteractions(tested.taskPersister);
		}

		// case - heartbeat performed, some running tasks in runner
		{
			Mockito.reset(tested.taskPersister);
			tested.runningTasks.put("tid1", Mockito.mock(Task.class));
			tested.runningTasks.put("tid2", Mockito.mock(Task.class));
			long lhb = System.currentTimeMillis() - tested.hbPeriod - 200;
			tested.lastHb = lhb;
			Mockito.doAnswer(new Answer() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					Set<String> s = (Set<String>) invocation.getArguments()[1];
					Assert.assertNotNull(s);
					Assert.assertEquals(2, s.size());
					Assert.assertTrue(s.contains("tid1"));
					Assert.assertTrue(s.contains("tid2"));
					return null;
				}
			}).when(tested.taskPersister).heartbeat(Mockito.eq(TESTNODEID), Mockito.anySet(), Mockito.anyLong());

			tested.heartbeat();

			TestUtils.assertCurrentDate(tested.lastHb);
			Mockito.verify(tested.taskPersister).heartbeat(Mockito.eq(TESTNODEID), Mockito.anySet(),
					Mockito.eq(tested.hbPeriod * 5));
			Mockito.verifyNoMoreInteractions(tested.taskPersister);
		}

	}

	@Test
	public void cancelTask() throws InterruptedException {
		TaskRunner tested = getTested();
		tested.cancelTask("unknown");

		Task t1 = createTaskMock(true);
		Task t2 = createTaskMock(false);
		tested.runningTasks.put("t1", t1);
		tested.runningTasks.put("t2", t2);
		Thread.sleep(700);

		tested.cancelTask("t1");
		tested.cancelTask("t2");
		Assert.assertTrue(t1.isCanceledOrInterrupted());
		Assert.assertFalse(t2.isCanceledOrInterrupted());
	}

	@Test
	public void removeFinished() throws InterruptedException {
		TaskRunner tested = getTested();

		tested.removeFinished();
		Assert.assertEquals(0, tested.runningTasks.size());

		Task t1 = createTaskMock(true);
		Task t3 = createTaskMock(true);

		tested.runningTasks.put("t1", t1);
		tested.runningTasks.put("t2", createTaskMock(false));
		tested.runningTasks.put("t3", t3);
		tested.runningTasks.put("t4", createTaskMock(false));
		Thread.sleep(1000);
		tested.removeFinished();
		Assert.assertEquals(2, tested.runningTasks.size());
		Assert.assertEquals(t1, tested.runningTasks.get("t1"));
		Assert.assertEquals(t3, tested.runningTasks.get("t3"));
	}

	@Test
	public void TaskExecutionContextImpl() {
		TaskRunner tested = new TaskRunner();
		tested.taskPersister = Mockito.mock(TaskPersister.class);

		tested.taskExecutionContextInstance.changeTaskStatus("aa", TaskStatus.CANCELED, "mymessage");
		Mockito.verify(tested.taskPersister).changeTaskStatus("aa", TaskStatus.CANCELED, "mymessage");

		tested.taskExecutionContextInstance.writeTaskLog("aaa", "msg");
		Mockito.verify(tested.taskPersister).writeTaskLog("aaa", "msg");

	}

	private TaskRunner getTested() {
		TaskRunner tested = new TaskRunner();
		tested.taskPersister = Mockito.mock(TaskPersister.class);
		tested.taskFactory = Mockito.mock(TaskFactory.class);
		tested.taskExecutionContextInstance = Mockito.mock(TaskExecutionContext.class);
		tested.maxRunningTasks = 2;
		tested.nodeId = TESTNODEID;
		return tested;
	}

	private Task createTaskMock(final boolean alive) {
		Task t = new Task() {
			@Override
			protected void performTask() throws Exception {
			}

			@Override
			public void run() {
				if (alive) {
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
					}
				}
			}
		};
		t.setDaemon(true);
		t.start();
		return t;
	}
}
