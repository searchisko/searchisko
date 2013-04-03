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
		DcpTaskFactory tested = getTested();

		try {
			tested.createTask("nonsense", null);
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}

	}

	@Test
	public void createTask_REINDEX_FROM_PERSISTENCE() throws TaskConfigurationException, UnsupportedTaskException {
		DcpTaskFactory tested = getTested();

		// case - missing content type in configuration
		try {
			tested.createTask(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), null);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing content type in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			tested.createTask(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing content type in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(DcpTaskFactory.CFG_DCP_CONTENT_TYPE, "  ");
			tested.createTask(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("dcp_content_type configuration property must be defined", e.getMessage());
		}

		// case - nonexisting content type in configuration
		{
			Mockito.when(tested.providerService.findContentType("mytype")).thenReturn(null);
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(DcpTaskFactory.CFG_DCP_CONTENT_TYPE, "mytype");

			try {
				tested.createTask(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			} catch (TaskConfigurationException e) {
				Assert.assertEquals("Content type 'mytype' doesn't exists.", e.getMessage());
			}
		}

		// case - nonpersistent content type in configuration
		{
			Map<String, Object> typeDef = new HashMap<String, Object>();
			typeDef.put(ProviderService.PERSIST, false);
			Mockito.when(tested.providerService.findContentType("mytype")).thenReturn(typeDef);
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(DcpTaskFactory.CFG_DCP_CONTENT_TYPE, "mytype");

			try {
				tested.createTask(DcpTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			} catch (TaskConfigurationException e) {
				Assert.assertEquals("Content type 'mytype' is not persisted.", e.getMessage());
			}
		}

		// case - everything is OK
		{
			Map<String, Object> typeDef = new HashMap<String, Object>();
			typeDef.put(ProviderService.PERSIST, true);
			Mockito.when(tested.providerService.findContentType("mytype")).thenReturn(typeDef);

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

	private DcpTaskFactory getTested() {
		DcpTaskFactory tested = new DcpTaskFactory();
		tested.contentPersistenceService = Mockito.mock(ContentPersistenceService.class);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		return tested;
	}

}
