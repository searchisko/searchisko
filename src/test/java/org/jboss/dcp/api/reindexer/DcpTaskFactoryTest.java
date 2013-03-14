/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.tasker.Task;
import org.jboss.dcp.api.tasker.TaskConfigurationException;
import org.jboss.dcp.api.tasker.UnsupportedTaskException;
import org.jboss.dcp.persistence.service.ContentPersistenceService;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link DcpTaskFactory}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class DcpTaskFactoryTest {

	@Test
	public void listSupportedTaskTypes() {
		DcpTaskFactory tested = new DcpTaskFactory();
		List<String> t = tested.listSupportedTaskTypes();
		Assert.assertEquals(DcpTaskTypes.values().length, t.size());
		Assert.assertTrue(t.contains(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType()));
	}

	@Test
	public void createTask() throws TaskConfigurationException, UnsupportedTaskException {
		DcpTaskFactory tested = new DcpTaskFactory();
		tested.contentPersistenceService = Mockito.mock(ContentPersistenceService.class);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.searchClientService = Mockito.mock(SearchClientService.class);

		try {
			tested.createTask("nonsense", null);
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}

		// case - REINDEX_FROM_PERSISTENCE tests
		try {
			tested.createTask(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), null);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			tested.createTask(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(DcpTaskFactory.CFG_DCP_CONTENT_TYPE, "mytype");
			Task task = tested.createTask(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			Assert.assertEquals(ReindexFromPersistenceTask.class, task.getClass());
			ReindexFromPersistenceTask ctask = (ReindexFromPersistenceTask) task;
			Assert.assertEquals("mytype", ctask.dcpContentType);
			Assert.assertEquals(tested.contentPersistenceService, ctask.contentPersistenceService);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

	}
}
