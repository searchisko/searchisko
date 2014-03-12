/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.tasker.TaskFactory;
import org.searchisko.api.tasker.TaskManager;
import org.searchisko.api.tasker.TaskPersister;

/**
 * Unit test {@link TaskService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class TaskServiceTest {

	@Test
	public void getTaskManager() {
		TaskService tested = getTested();
		tested.taskManager = Mockito.mock(TaskManager.class);
		Assert.assertEquals(tested.taskManager, tested.getTaskManager());
	}

	@Test
	public void initAndDestroy() {
		TaskService tested = getTested();

		Assert.assertNull(tested.taskManager);

		// no npe when not started
		tested.destroy();
		Assert.assertNull(tested.taskManager);

		// init
		tested.init();
		Assert.assertNotNull(tested.taskManager);
		Assert.assertNotNull(tested.taskManager.getTaskRunner());
		Assert.assertNotNull(tested.taskManager.getNodeId());
		Assert.assertEquals(tested.taskFactory, tested.taskManager.getTaskFactory());
		Assert.assertEquals(tested.taskPersister, tested.taskManager.getTaskPersister());

		// case - next init do not change manager
		TaskManager tm = tested.taskManager;
		tested.init();
		Assert.assertEquals(tm, tested.taskManager);

		// case - destroy
		tested.destroy();
		Assert.assertNull(tested.taskManager);
		Assert.assertNull(tm.getTaskRunner());

		// case - node id for jboss server
		System.setProperty("jboss.node.name", "jbnd");
		tested.init();
		Assert.assertEquals("jbnd", tested.taskManager.getNodeId());
		tested.destroy();
	}

	private TaskService getTested() {
		TaskService tested = new TaskService();
		tested.log = Logger.getLogger("testlogger");
		tested.taskFactory = Mockito.mock(TaskFactory.class);
		tested.taskPersister = Mockito.mock(TaskPersister.class);
		return tested;
	}
}
