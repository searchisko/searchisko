/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.tasker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.JpaTestBase;

/**
 * Unit test for {@link TaskPersisterJpa} using embedded DB.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskPersisterJpaTest_Embedded extends JpaTestBase {

	{
		logger = Logger.getLogger(TaskPersisterJpaTest_Embedded.class.getName());
	}

	@Test
	public void heartbeat() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;
		try {
			em.getTransaction().begin();
			tested.heartbeat("mynode", null, 1000);
			em.getTransaction().commit();

			em.getTransaction().begin();
			long now = System.currentTimeMillis();

			// not expired on same node - no FAILOVER
			String id1 = tested.createTask("type1", null);
			TaskStatusInfo tsi = em.find(TaskStatusInfo.class, id1);
			tsi.startTaskExecution("mynode");
			tsi.setHeartbeat(now - 2500);

			// expired on same node but running still here - no FAILOVER
			String id2 = tested.createTask("type1", null);
			tsi = em.find(TaskStatusInfo.class, id2);
			tsi.startTaskExecution("mynode");
			tsi.setHeartbeat(now - 3200);

			// expired on same node, no running here anymore - must be FAILOVER
			String id3 = tested.createTask("type1", null);
			tsi = em.find(TaskStatusInfo.class, id3);
			tsi.startTaskExecution("mynode");
			tsi.setHeartbeat(now - 3200);

			// not expired on another node - no FAILOVER
			String id4 = tested.createTask("type1", null);
			tsi = em.find(TaskStatusInfo.class, id4);
			tsi.startTaskExecution("mynode-other");
			tsi.setHeartbeat(now - 2500);

			// expired on another node - must be FAILOVER
			String id5 = tested.createTask("type1", null);
			tsi = em.find(TaskStatusInfo.class, id5);
			tsi.startTaskExecution("mynode-other");
			tsi.setHeartbeat(now - 3200);

			// not running task so no FAILOVER
			String id6 = tested.createTask("type1", null);
			em.getTransaction().commit();

			// perform hertbeat
			em.getTransaction().begin();
			Set<String> runningTasksId = new HashSet<String>();
			runningTasksId.add(id1);
			runningTasksId.add(id2);
			tested.heartbeat("mynode", runningTasksId, 3000);
			em.getTransaction().commit();

			// assert outputs
			em.getTransaction().begin();
			tsi = em.find(TaskStatusInfo.class, id1);
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			TestUtils.assertCurrentDate(tsi.getHeartbeat());

			tsi = em.find(TaskStatusInfo.class, id2);
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			TestUtils.assertCurrentDate(tsi.getHeartbeat());

			tsi = em.find(TaskStatusInfo.class, id3);
			Assert.assertEquals(TaskStatus.FAILOVER, tsi.getTaskStatus());
			TestUtils.assertCurrentDate(tsi.getLastRunFinishedAt());

			tsi = em.find(TaskStatusInfo.class, id4);
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			Assert.assertEquals(now - 2500, tsi.getHeartbeat());

			tsi = em.find(TaskStatusInfo.class, id5);
			Assert.assertEquals(TaskStatus.FAILOVER, tsi.getTaskStatus());
			TestUtils.assertCurrentDate(tsi.getLastRunFinishedAt());

			tsi = em.find(TaskStatusInfo.class, id6);
			Assert.assertEquals(TaskStatus.NEW, tsi.getTaskStatus());

			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void getTaskToRun() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {

			em.getTransaction().begin();
			TaskStatusInfo tsi = tested.getTaskToRun("mynode");
			Assert.assertNull(tsi);
			em.getTransaction().commit();

			em.getTransaction().begin();
			String id = tested.createTask("type1", null);
			String id1 = tested.createTask("type1", null);
			Thread.sleep(100);
			String id2 = tested.createTask("type1", null);
			em.getTransaction().commit();

			em.getTransaction().begin();
			tested.getTaskStatusInfo(id).setTaskStatus(TaskStatus.FINISHED_OK);
			tsi = tested.getTaskStatusInfo(id2);
			tsi.setTaskStatus(TaskStatus.FAILOVER);
			tsi.setRunCount(1);
			tsi.setLastRunFinishedAt(tsi.getTaskCreatedAt());
			tsi.setLastRunStartedAt(tsi.getTaskCreatedAt());
			em.getTransaction().commit();

			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode");
			Assert.assertNotNull(tsi);
			Assert.assertEquals(id1, tsi.getId());
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			Assert.assertEquals(1, tsi.getRunCount());
			em.getTransaction().commit();

			// id2 is started now as it is first failover
			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode");
			Assert.assertNotNull(tsi);
			Assert.assertEquals(id2, tsi.getId());
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			Assert.assertEquals(2, tsi.getRunCount());
			em.getTransaction().commit();

			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode");
			Assert.assertNull(tsi);
			em.getTransaction().commit();

			// task 2 failed again
			em.getTransaction().begin();
			tsi = tested.getTaskStatusInfo(id2);
			tsi.setTaskStatus(TaskStatus.FAILOVER);
			tsi.setRunCount(1);
			tsi.setExecutionNodeId("mynode");
			tsi.setLastRunFinishedAt(new Date());
			tsi.setLastRunStartedAt(tsi.getTaskCreatedAt());
			em.getTransaction().commit();

			// it is not selected on same node just now as we have timeout
			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode");
			Assert.assertNull(tsi);
			em.getTransaction().commit();

			// is selected now on another node due cluster failover
			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode2");
			Assert.assertNotNull(tsi);
			Assert.assertEquals(id2, tsi.getId());
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			Assert.assertEquals(2, tsi.getRunCount());
			em.getTransaction().commit();

			// set it back to failover mode with older rlast finish date
			em.getTransaction().begin();
			tsi = tested.getTaskStatusInfo(id2);
			tsi.setTaskStatus(TaskStatus.FAILOVER);
			tsi.setRunCount(1);
			tsi.setLastRunFinishedAt(new Date(System.currentTimeMillis() - TaskPersisterJpa.FAILOVER_DELAY_10 - 100L));
			tsi.setLastRunStartedAt(tsi.getTaskCreatedAt());
			em.getTransaction().commit();

			// is started now as delay is over
			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode");
			Assert.assertNotNull(tsi);
			Assert.assertEquals(id2, tsi.getId());
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			Assert.assertEquals(2, tsi.getRunCount());
			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void createTask_getTaskStatusInfo() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {
			em.getTransaction().begin();
			String id1 = tested.createTask("type1", null);
			Map<String, Object> tc2 = new HashMap<String, Object>();
			tc2.put("k1", "v1");
			String id2 = tested.createTask("type2", tc2);
			em.getTransaction().commit();

			em.getTransaction().begin();

			Assert.assertNull(tested.getTaskStatusInfo("unknown"));

			TaskStatusInfo ts1 = tested.getTaskStatusInfo(id1);
			Assert.assertNotNull(ts1);
			Assert.assertEquals("type1", ts1.getTaskType());
			Assert.assertEquals(TaskStatus.NEW, ts1.getTaskStatus());
			Assert.assertEquals(null, ts1.getTaskConfig());

			TaskStatusInfo ts2 = tested.getTaskStatusInfo(id2);
			Assert.assertNotNull(ts2);
			Assert.assertEquals("type2", ts2.getTaskType());
			Assert.assertEquals(TaskStatus.NEW, ts2.getTaskStatus());
			TestUtils.assertJsonContent(tc2, ts2.getTaskConfig());

			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void changeTaskStatus() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {
			em.getTransaction().begin();
			String id1 = tested.createTask("type1", null);
			em.getTransaction().commit();

			em.getTransaction().begin();
			tested.changeTaskStatus(id1, TaskStatus.RUNNING, "starting");
			em.getTransaction().commit();

			em.getTransaction().begin();
			TaskStatusInfo ts = tested.getTaskStatusInfo(id1);
			Assert.assertEquals(TaskStatus.RUNNING, ts.getTaskStatus());
			Assert.assertEquals("starting", ts.getProcessingLog());
			em.getTransaction().commit();

			em.getTransaction().begin();
			tested.changeTaskStatus(id1, TaskStatus.FINISHED_OK, "finished");
			em.getTransaction().commit();

			em.getTransaction().begin();
			ts = tested.getTaskStatusInfo(id1);
			Assert.assertEquals(TaskStatus.FINISHED_OK, ts.getTaskStatus());
			Assert.assertEquals("starting\nfinished", ts.getProcessingLog());
			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void writeTaskLog() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {
			em.getTransaction().begin();
			String id1 = tested.createTask("type1", null);
			em.getTransaction().commit();

			em.getTransaction().begin();
			tested.writeTaskLog(id1, "starting");
			em.getTransaction().commit();

			em.getTransaction().begin();
			TaskStatusInfo ts = tested.getTaskStatusInfo(id1);
			Assert.assertEquals("starting", ts.getProcessingLog());
			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void markTaskToBeCancelled() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {
			em.getTransaction().begin();
			String id1 = tested.createTask("type1", null);
			em.getTransaction().commit();

			em.getTransaction().begin();
			tested.markTaskToBeCancelled(id1);
			em.getTransaction().commit();

			em.getTransaction().begin();
			TaskStatusInfo ts = tested.getTaskStatusInfo(id1);
			Assert.assertEquals(true, ts.isCancelRequested());
			em.getTransaction().commit();

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void listTasks_basicTests_ordering() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {

			// case - select from empty table
			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks(null, null, 0, 0);
				Assert.assertEquals(0, ret.size());
				em.getTransaction().commit();
			}

			prepareTestData();

			// case - result ordering
			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks(null, null, 0, 0);
				Assert.assertEquals(8, ret.size());
				Assert.assertEquals("a4", ret.get(0).getId());
				Assert.assertEquals("a3", ret.get(1).getId());
				Assert.assertEquals("b1", ret.get(7).getId());
				em.getTransaction().commit();
			}

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void listTasks_pager() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {

			prepareTestData();

			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks(null, null, 0, 3);
				Assert.assertEquals(3, ret.size());
				Assert.assertEquals("a4", ret.get(0).getId());
				Assert.assertEquals("a3", ret.get(1).getId());
				Assert.assertEquals("b4", ret.get(2).getId());
				em.getTransaction().commit();
			}
			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks(null, null, 2, 3);
				Assert.assertEquals(3, ret.size());
				Assert.assertEquals("b4", ret.get(0).getId());
				Assert.assertEquals("b3", ret.get(1).getId());
				Assert.assertEquals("a2", ret.get(2).getId());
				em.getTransaction().commit();
			}
			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks(null, null, 4, 0);
				Assert.assertEquals(4, ret.size());
				Assert.assertEquals("a2", ret.get(0).getId());
				Assert.assertEquals("a1", ret.get(1).getId());
				Assert.assertEquals("b2", ret.get(2).getId());
				Assert.assertEquals("b1", ret.get(3).getId());
				em.getTransaction().commit();
			}
			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks(null, null, 4, 10);
				Assert.assertEquals(4, ret.size());
				Assert.assertEquals("a2", ret.get(0).getId());
				Assert.assertEquals("a1", ret.get(1).getId());
				Assert.assertEquals("b2", ret.get(2).getId());
				Assert.assertEquals("b1", ret.get(3).getId());
				em.getTransaction().commit();
			}
			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks(null, null, -1, -1);
				Assert.assertEquals(8, ret.size());
				Assert.assertEquals("a4", ret.get(0).getId());
				Assert.assertEquals("b1", ret.get(7).getId());
				em.getTransaction().commit();
			}

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void listTasks_filterByType() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {

			prepareTestData();

			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks(" ", null, 0, 0);
				Assert.assertEquals(8, ret.size());
				em.getTransaction().commit();
			}
			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks("typeunknown", null, 0, 0);
				Assert.assertEquals(0, ret.size());
				em.getTransaction().commit();
			}
			{
				em.getTransaction().begin();
				List<TaskStatusInfo> ret = tested.listTasks("type1", null, 0, 0);
				Assert.assertEquals(4, ret.size());
				Assert.assertEquals("a3", ret.get(0).getId());
				Assert.assertEquals("b3", ret.get(1).getId());
				Assert.assertEquals("a1", ret.get(2).getId());
				Assert.assertEquals("b1", ret.get(3).getId());
				em.getTransaction().commit();
			}
		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void listTasks_filterByStatus() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {

			prepareTestData();

			{
				em.getTransaction().begin();
				List<TaskStatus> filter = new ArrayList<TaskStatus>();
				List<TaskStatusInfo> ret = tested.listTasks(null, filter, 0, 0);
				Assert.assertEquals(8, ret.size());
				em.getTransaction().commit();
			}
			{
				em.getTransaction().begin();
				List<TaskStatus> filter = new ArrayList<TaskStatus>();
				filter.add(TaskStatus.FINISHED_OK);
				List<TaskStatusInfo> ret = tested.listTasks(null, filter, 0, 0);
				Assert.assertEquals(2, ret.size());
				Assert.assertEquals("b4", ret.get(0).getId());
				Assert.assertEquals("b3", ret.get(1).getId());
				em.getTransaction().commit();
			}
			{
				em.getTransaction().begin();
				List<TaskStatus> filter = new ArrayList<TaskStatus>();
				filter.add(TaskStatus.FINISHED_OK);
				filter.add(TaskStatus.FINISHED_ERROR);
				filter.add(TaskStatus.RUNNING);
				List<TaskStatusInfo> ret = tested.listTasks(null, filter, 0, 0);
				Assert.assertEquals(4, ret.size());
				Assert.assertEquals("a3", ret.get(0).getId());
				Assert.assertEquals("b4", ret.get(1).getId());
				Assert.assertEquals("b3", ret.get(2).getId());
				Assert.assertEquals("a1", ret.get(3).getId());
				em.getTransaction().commit();
			}

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	@Test
	public void listTasks_filterCombination() {
		TaskPersisterJpa tested = new TaskPersisterJpa();
		tested.em = em;

		try {

			prepareTestData();

			{
				// AND semantic for filters
				em.getTransaction().begin();
				List<TaskStatus> filter = new ArrayList<TaskStatus>();
				filter.add(TaskStatus.FINISHED_OK);
				filter.add(TaskStatus.FINISHED_ERROR);
				filter.add(TaskStatus.RUNNING);
				List<TaskStatusInfo> ret = tested.listTasks("type2", filter, 0, 0);
				Assert.assertEquals(1, ret.size());
				Assert.assertEquals("b4", ret.get(0).getId());
				em.getTransaction().commit();
			}

		} catch (Exception ex) {
			em.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			Assert.fail("Exception during testPersistence, see log file");
		}
	}

	private void prepareTestData() {
		em.getTransaction().begin();
		createTaskStatusInfoData("b2", "type2", TaskStatus.NEW, 9000);
		createTaskStatusInfoData("b1", "type1", TaskStatus.NEW, 10000);
		createTaskStatusInfoData("a1", "type1", TaskStatus.RUNNING, 8000);
		createTaskStatusInfoData("a2", "type2", TaskStatus.CANCELED, 7000);
		createTaskStatusInfoData("b3", "type1", TaskStatus.FINISHED_OK, 6000);
		createTaskStatusInfoData("b4", "type2", TaskStatus.FINISHED_OK, 5000);
		createTaskStatusInfoData("a4", "type2", TaskStatus.CANCELED, 3000);
		createTaskStatusInfoData("a3", "type1", TaskStatus.FINISHED_ERROR, 4000);

		em.getTransaction().commit();
	}

	private void createTaskStatusInfoData(String id, String taskType, TaskStatus taskStatus, long timeShift) {
		TaskStatusInfo tsi = new TaskStatusInfo();
		tsi.setTaskCreatedAt(new Date(System.currentTimeMillis() - timeShift));
		tsi.setId(id);
		tsi.setTaskType(taskType);
		tsi.setTaskStatus(taskStatus);
		em.persist(tsi);
	}

}
