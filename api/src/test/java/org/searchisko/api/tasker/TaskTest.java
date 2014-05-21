/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.io.InterruptedIOException;

import org.elasticsearch.index.Index;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for {@link Task}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskTest {

	private static final String TASK_ID = "task id";

	@Test
	public void run() throws InterruptedException {

		// case - finished OK
		{
			Task tested = getTested();
			tested.run();
			Mockito.verify(tested.context).changeTaskStatus(TASK_ID, TaskStatus.FINISHED_OK, null);
		}

		// case - canceled
		{
			Task tested = getTested();
			tested.setCanceled(true);
			tested.run();
			Mockito.verify(tested.context).changeTaskStatus(TASK_ID, TaskStatus.CANCELED, null);
		}

		// case - runtime exception
		{
			Task tested = getTested(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					throw new RuntimeException("my runtime exception message");
				}
			});
			tested.run();
			Mockito.verify(tested.context).changeTaskStatus(TASK_ID, TaskStatus.FAILOVER,
					"ERROR: Task execution interrupted due java.lang.RuntimeException: my runtime exception message");
		}

		// case - index missing exception
		{
			Task tested = getTested(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					throw new IndexMissingException(new Index("my_index"));
				}
			});
			tested.run();
			Mockito.verify(tested.context).changeTaskStatus(TASK_ID, TaskStatus.FINISHED_ERROR,
					"ERROR: Task finished due missing search index: [my_index] missing");
		}

		// case - null pointer exception
		{
			Task tested = getTested(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					throw new NullPointerException();
				}
			});
			tested.run();
			Mockito.verify(tested.context).changeTaskStatus(TASK_ID, TaskStatus.FAILOVER,
					"ERROR: Task execution interrupted due NullPointerException, see log file for stacktrace");
		}

		// case - checked exception
		{
			Task tested = getTested(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					throw new Exception("my exception message");
				}
			});
			tested.run();
			Mockito.verify(tested.context).changeTaskStatus(TASK_ID, TaskStatus.FINISHED_ERROR,
					"ERROR: Task finished due exception: my exception message");
		}

		// case - interrupted
		{
			Task tested = getTested(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					throw new InterruptedException();
				}
			});
			tested.run();
			Mockito.verify(tested.context).changeTaskStatus(TASK_ID, TaskStatus.FAILOVER, "Task execution was interrupted");
		}

		// case - interrupted IO
		{
			Task tested = getTested(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					throw new InterruptedIOException();
				}
			});
			tested.run();
			Mockito.verify(tested.context).changeTaskStatus(TASK_ID, TaskStatus.FAILOVER, "Task execution was interrupted");
		}

	}

	@Test
	public void isCanceledOrInterrupted() throws InterruptedException {
		Task tested = getTested();
		Assert.assertFalse(tested.isCanceledOrInterrupted());
		tested.setCanceled(true);
		Assert.assertTrue(tested.isCanceledOrInterrupted());
		tested.setCanceled(false);
		Assert.assertFalse(tested.isCanceledOrInterrupted());
	}

	@Test
	public void writeTaskLog() {
		Task tested = getTested();
		tested.taskId = "taskid";
		tested.writeTaskLog("mymessage");
		Mockito.verify(tested.context).writeTaskLog(tested.taskId, "mymessage");
	}

	@Test
	public void setExecutionContext() {
		Task tested = getTested();
		TaskExecutionContext context = Mockito.mock(TaskExecutionContext.class);
		tested.setExecutionContext("mytaskid", context);

		Assert.assertEquals("mytaskid", tested.taskId);
		Assert.assertEquals(context, tested.context);
		Assert.assertFalse(tested.isDaemon());

	}

	private Task getTested() {
		return getTested(null);
	}

	private Task getTested(final Answer<Boolean> answer) {
		Task t = new Task() {

			@Override
			protected void performTask() throws Exception {
				try {
					if (answer != null)
						answer.answer(null);
				} catch (Exception e) {
					throw e;
				} catch (Throwable t) {
					throw new Exception(t);
				}
			}

		};
		t.taskId = TASK_ID;
		t.context = Mockito.mock(TaskExecutionContext.class);
		return t;
	}

}
