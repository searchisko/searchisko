/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.tasker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.dcp.api.testtools.TestUtils;
import org.jboss.dcp.persistence.service.JpaTestBase;
import org.junit.Assert;
import org.junit.Test;

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
			tsi.setRunCount(2);
			em.getTransaction().commit();

			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode");
			Assert.assertNotNull(tsi);
			Assert.assertEquals(id1, tsi.getId());
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			Assert.assertEquals(1, tsi.getRunCount());
			em.getTransaction().commit();

			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode");
			Assert.assertNotNull(tsi);
			Assert.assertEquals(id2, tsi.getId());
			Assert.assertEquals(TaskStatus.RUNNING, tsi.getTaskStatus());
			Assert.assertEquals(3, tsi.getRunCount());
			em.getTransaction().commit();

			em.getTransaction().begin();
			tsi = tested.getTaskToRun("mynode");
			Assert.assertNull(tsi);
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
			Assert.assertEquals(true, ts.isCancelRequsted());
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
