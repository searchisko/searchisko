/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link TaskStatusInfo}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskStatusInfoTest {

	@Test
	public void getTaskConfig() {
		TaskStatusInfo tested = new TaskStatusInfo();

		Assert.assertNull(tested.getTaskConfig());

		tested.taskConfigSerialized = "{\"testkey\":\"testvalue\"}";
		Map<String, Object> t = tested.getTaskConfig();
		Assert.assertNotNull(t);
		Assert.assertEquals("testvalue", t.get("testkey"));

		// error in JSON
		try {
			tested.taskConfigSerialized = "{\"testkey\":\"testvalue}";
			tested.getTaskConfig();
			Assert.fail("RuntimeException expected");
		} catch (RuntimeException e) {

		}
	}

	@Test
	public void setTaskConfig() throws IOException {
		TaskStatusInfo tested = new TaskStatusInfo();

		tested.taskConfigSerialized = "aaaa";
		tested.setTaskConfig(null);
		Assert.assertNull(tested.taskConfigSerialized);

		tested.setTaskConfig(new HashMap<String, Object>());
		Assert.assertEquals("{}", tested.taskConfigSerialized);

		Map<String, Object> m = new HashMap<String, Object>();
		m.put("mk", "mv");
		tested.setTaskConfig(m);
		TestUtils.assertJsonContent("{\"mk\":\"mv\"}", tested.taskConfigSerialized);
	}

	@Test
	public void appendProcessingLog() {
		TaskStatusInfo tested = new TaskStatusInfo();
		Assert.assertNull(tested.processingLog);

		tested.appendProcessingLog(null);
		Assert.assertNull(tested.processingLog);

		tested.appendProcessingLog("   ");
		Assert.assertNull(tested.processingLog);

		tested.appendProcessingLog("row1");
		Assert.assertEquals("row1", tested.processingLog);

		tested.appendProcessingLog("row2");
		Assert.assertEquals("row1\nrow2", tested.processingLog);

		tested.appendProcessingLog(null);
		Assert.assertEquals("row1\nrow2", tested.processingLog);
		tested.appendProcessingLog("   ");
		Assert.assertEquals("row1\nrow2", tested.processingLog);

		tested.appendProcessingLog("row3");
		Assert.assertEquals("row1\nrow2\nrow3", tested.processingLog);

		// test long log stripping
		tested.processingLog = "bbb";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 65000; i++) {
			sb.append("a");
		}
		tested.appendProcessingLog(sb.toString());
		Assert.assertEquals(65000, tested.processingLog.length());
		Assert.assertEquals('a', tested.processingLog.charAt(0));

	}

	@Test
	public void startTaskExecution() {
		TaskStatusInfo tested = new TaskStatusInfo();

		assertStartRTaskExecutionIllegalStateException(tested);
		tested.taskStatus = TaskStatus.RUNNING;
		assertStartRTaskExecutionIllegalStateException(tested);
		tested.taskStatus = TaskStatus.CANCELED;
		assertStartRTaskExecutionIllegalStateException(tested);
		tested.taskStatus = TaskStatus.FINISHED_OK;
		assertStartRTaskExecutionIllegalStateException(tested);
		tested.taskStatus = TaskStatus.FINISHED_ERROR;
		assertStartRTaskExecutionIllegalStateException(tested);

		// case - start ok for new
		{
			tested = new TaskStatusInfo();
			tested.taskStatus = TaskStatus.NEW;
			Assert.assertTrue(tested.startTaskExecution("mynode"));

			Assert.assertEquals(1, tested.runCount);
			Assert.assertEquals(TaskStatus.RUNNING, tested.taskStatus);
			Assert.assertEquals("mynode", tested.executionNodeId);
			TestUtils.assertCurrentDate(tested.lastRunStartedAt);
			TestUtils.assertCurrentDate(tested.heartbeat);
			Assert.assertNull(tested.lastRunFinishedAt);
		}

		// case - start ok for failover if count is under max count
		{
			tested = new TaskStatusInfo();
			tested.taskStatus = TaskStatus.FAILOVER;
			tested.runCount = TaskStatusInfo.FAILOVER_MAX_NUM - 1;
			tested.lastRunStartedAt = new Date(System.currentTimeMillis() - 100000l);
			tested.lastRunFinishedAt = new Date(System.currentTimeMillis() - 90000l);
			Assert.assertTrue(tested.startTaskExecution("mynode2"));

			Assert.assertEquals(TaskStatusInfo.FAILOVER_MAX_NUM, tested.runCount);
			Assert.assertEquals(TaskStatus.RUNNING, tested.taskStatus);
			Assert.assertEquals("mynode2", tested.executionNodeId);
			TestUtils.assertCurrentDate(tested.lastRunStartedAt);
			TestUtils.assertCurrentDate(tested.heartbeat);
			Assert.assertNull(tested.lastRunFinishedAt);
		}

		// case - no start due cancel request, cancel it directly
		{
			tested = new TaskStatusInfo();
			tested.taskStatus = TaskStatus.NEW;
			tested.setCancelRequested(true);
			Assert.assertFalse(tested.startTaskExecution("mynode"));

			Assert.assertEquals(TaskStatus.CANCELED, tested.taskStatus);
			Assert.assertEquals(0, tested.runCount);
			Assert.assertNull(tested.executionNodeId);
			Assert.assertNull(tested.lastRunStartedAt);
			Assert.assertNull(tested.lastRunFinishedAt);
		}

		{
			tested = new TaskStatusInfo();
			tested.taskStatus = TaskStatus.FAILOVER;
			tested.setCancelRequested(true);
			Assert.assertFalse(tested.startTaskExecution("mynode"));

			Assert.assertEquals(TaskStatus.CANCELED, tested.taskStatus);
			Assert.assertEquals(0, tested.runCount);
			Assert.assertNull(tested.executionNodeId);
			Assert.assertNull(tested.lastRunStartedAt);
			Assert.assertNull(tested.lastRunFinishedAt);
		}

		// case - automatic cancel due too much failover attempts
		{
			tested = new TaskStatusInfo();
			tested.taskStatus = TaskStatus.FAILOVER;
			tested.setCancelRequested(false);
			tested.setRunCount(TaskStatusInfo.FAILOVER_MAX_NUM);
			Assert.assertFalse(tested.startTaskExecution("mynode"));

			Assert.assertEquals(TaskStatus.CANCELED, tested.taskStatus);
			Assert.assertEquals(TaskStatusInfo.FAILOVER_MAX_NUM, tested.runCount);
			Assert.assertNull(tested.executionNodeId);
			Assert.assertNull(tested.lastRunStartedAt);
			Assert.assertNull(tested.lastRunFinishedAt);
		}

	}

	private void assertStartRTaskExecutionIllegalStateException(TaskStatusInfo tested) {
		Assert.assertFalse(tested.startTaskExecution("mynode"));
	}

}
