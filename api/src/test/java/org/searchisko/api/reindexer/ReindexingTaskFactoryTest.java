/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.service.ContributorService;
import org.searchisko.api.service.ProjectService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.tasker.Task;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.UnsupportedTaskException;
import org.searchisko.persistence.service.ContentPersistenceService;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ReindexingTaskFactory}.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ReindexingTaskFactoryTest {

	@Test
	public void listSupportedTaskTypes() {
		ReindexingTaskFactory tested = new ReindexingTaskFactory();
		List<String> t = tested.listSupportedTaskTypes();
		Assert.assertEquals(ReindexingTaskTypes.values().length, t.size());
		Assert.assertTrue(t.contains(ReindexingTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType()));
	}

	@Test
	public void createTask() throws TaskConfigurationException, UnsupportedTaskException {
		ReindexingTaskFactory tested = getTested();

		try {
			tested.createTask("nonsense", null);
			Assert.fail("UnsupportedTaskException expected");
		} catch (UnsupportedTaskException e) {
			// OK
		}

	}

	@Test
	public void createTask_REINDEX_FROM_PERSISTENCE() throws TaskConfigurationException, UnsupportedTaskException {
		ReindexingTaskFactory tested = getTested();

		// case - missing content type in configuration
		try {
			tested.createTask(ReindexingTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), null);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing content type in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			tested.createTask(ReindexingTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing content type in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_SYS_CONTENT_TYPE, "  ");
			tested.createTask(ReindexingTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("sys_content_type configuration property must be defined", e.getMessage());
		}

		// case - nonexisting content type in configuration
		{
			Mockito.when(tested.providerService.findContentType("mytype")).thenReturn(null);
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_SYS_CONTENT_TYPE, "mytype");

			try {
				tested.createTask(ReindexingTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
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
			config.put(ReindexingTaskFactory.CFG_SYS_CONTENT_TYPE, "mytype");

			try {
				tested.createTask(ReindexingTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
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
			config.put(ReindexingTaskFactory.CFG_SYS_CONTENT_TYPE, "mytype");
			Task task = tested.createTask(ReindexingTaskTypes.REINDEX_FROM_PERSISTENCE.getTaskType(), config);
			Assert.assertEquals(ReindexFromPersistenceTask.class, task.getClass());
			ReindexFromPersistenceTask ctask = (ReindexFromPersistenceTask) task;
			Assert.assertEquals("mytype", ctask.sysContentType);
			Assert.assertEquals(tested.contentPersistenceService, ctask.contentPersistenceService);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}
	}

	@Test
	public void createTask_RENORMALIZE_BY_CONTENT_TYPE() throws TaskConfigurationException, UnsupportedTaskException {
		ReindexingTaskFactory tested = getTested();

		// case - missing content type in configuration
		try {
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTENT_TYPE.getTaskType(), null);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing content type in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTENT_TYPE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing content type in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_SYS_CONTENT_TYPE, "  ");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTENT_TYPE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("sys_content_type configuration property must be defined", e.getMessage());
		}

		// case - nonexisting content type in configuration
		{
			Mockito.when(tested.providerService.findContentType("mytype")).thenReturn(null);
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_SYS_CONTENT_TYPE, "mytype");

			try {
				tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTENT_TYPE.getTaskType(), config);
			} catch (TaskConfigurationException e) {
				Assert.assertEquals("Content type 'mytype' doesn't exists.", e.getMessage());
			}
		}

		// case - everything is OK
		{
			Map<String, Object> typeDef = new HashMap<String, Object>();
			Mockito.when(tested.providerService.findContentType("mytype")).thenReturn(typeDef);

			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_SYS_CONTENT_TYPE, "mytype");
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTENT_TYPE.getTaskType(), config);
			Assert.assertEquals(RenormalizeByContentTypeTask.class, task.getClass());
			RenormalizeByContentTypeTask ctask = (RenormalizeByContentTypeTask) task;
			Assert.assertEquals("mytype", ctask.sysContentType);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}
	}

	@Test
	public void createTask_RENORMALIZE_BY_PROJECT_CODE() throws TaskConfigurationException, UnsupportedTaskException {
		ReindexingTaskFactory tested = getTested();

		// case - missing project code in configuration
		try {
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), null);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing project code in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing project code in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_CODE, "  ");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_code configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_CODE, new String[] {});
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_code configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_CODE, new String[] { " " });
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_code configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_CODE, new ArrayList<String>());
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_code configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_CODE, Arrays.asList(new String[] { " ", "" }));
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_code configuration property must be defined", e.getMessage());
		}

		// case - everything is OK
		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_CODE, "myproject");
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsValueTask.class, task.getClass());
			RenormalizeByEsValueTask ctask = (RenormalizeByEsValueTask) task;
			Assert.assertEquals(ContentObjectFields.SYS_PROJECT, ctask.esField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_CODE, new String[] { "myproject", "myproject2" });
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsValueTask.class, task.getClass());
			RenormalizeByEsValueTask ctask = (RenormalizeByEsValueTask) task;
			Assert.assertEquals(ContentObjectFields.SYS_PROJECT, ctask.esField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals("myproject2", ctask.esValues[1]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_CODE, Arrays.asList(new String[] { "myproject", "myproject2" }));
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_CODE.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsValueTask.class, task.getClass());
			RenormalizeByEsValueTask ctask = (RenormalizeByEsValueTask) task;
			Assert.assertEquals(ContentObjectFields.SYS_PROJECT, ctask.esField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals("myproject2", ctask.esValues[1]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}
	}

	@Test
	public void createTask_RENORMALIZE_BY_CONTRIBUTOR_CODE() throws TaskConfigurationException, UnsupportedTaskException {
		ReindexingTaskFactory tested = getTested();

		// case - missing project code in configuration
		try {
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), null);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing project code in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing project code in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, "  ");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_code configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, new String[] {});
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_code configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, new String[] { " " });
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_code configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, new ArrayList<String>());
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_code configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, Arrays.asList(new String[] { " ", "" }));
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_code configuration property must be defined", e.getMessage());
		}

		// case - everything is OK
		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, "myproject");
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsValueTask.class, task.getClass());
			RenormalizeByEsValueTask ctask = (RenormalizeByEsValueTask) task;
			Assert.assertEquals(ContentObjectFields.SYS_CONTRIBUTORS, ctask.esField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, new String[] { "myproject", "myproject2" });
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsValueTask.class, task.getClass());
			RenormalizeByEsValueTask ctask = (RenormalizeByEsValueTask) task;
			Assert.assertEquals(ContentObjectFields.SYS_CONTRIBUTORS, ctask.esField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals("myproject2", ctask.esValues[1]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, Arrays.asList(new String[] { "myproject", "myproject2" }));
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsValueTask.class, task.getClass());
			RenormalizeByEsValueTask ctask = (RenormalizeByEsValueTask) task;
			Assert.assertEquals(ContentObjectFields.SYS_CONTRIBUTORS, ctask.esField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals("myproject2", ctask.esValues[1]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}
	}

	@Test
	public void createTask_RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID() throws TaskConfigurationException,
			UnsupportedTaskException {
		ReindexingTaskFactory tested = getTested();

		// case - missing project code in configuration
		try {
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), null);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing both config values in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}

		// case - missing CFG_CONTRIBUTOR_ID_TYPE in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, "value");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_type configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, "value");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_type configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "   ");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, "value");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_type configuration property must be defined", e.getMessage());
		}

		// case - missing CFG_CONTRIBUTOR_ID_VALUE in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, "  ");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, new String[] {});
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, new String[] { " " });
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, new ArrayList<String>());
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, Arrays.asList(new String[] { " ", "" }));
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("contributor_id_value configuration property must be defined", e.getMessage());
		}

		// case - everything is OK
		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, "idvalue");
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsLookedUpValuesTask.class, task.getClass());
			RenormalizeByEsLookedUpValuesTask ctask = (RenormalizeByEsLookedUpValuesTask) task;
			Assert.assertEquals(ContributorService.SEARCH_INDEX_NAME, ctask.lookupIndex);
			Assert.assertEquals(ContributorService.SEARCH_INDEX_TYPE, ctask.lookupType);
			Assert.assertEquals("idtype", ctask.lookupField);
			Assert.assertEquals("idvalue", ctask.esValues[0]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, new String[] { "myproject", "myproject2" });
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsLookedUpValuesTask.class, task.getClass());
			RenormalizeByEsLookedUpValuesTask ctask = (RenormalizeByEsLookedUpValuesTask) task;
			Assert.assertEquals(ContributorService.SEARCH_INDEX_NAME, ctask.lookupIndex);
			Assert.assertEquals(ContributorService.SEARCH_INDEX_TYPE, ctask.lookupType);
			Assert.assertEquals("idtype", ctask.lookupField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals("myproject2", ctask.esValues[1]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_ID_VALUE, Arrays.asList(new String[] { "myproject", "myproject2" }));
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_LOOKUP_ID.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsLookedUpValuesTask.class, task.getClass());
			RenormalizeByEsLookedUpValuesTask ctask = (RenormalizeByEsLookedUpValuesTask) task;
			Assert.assertEquals(ContributorService.SEARCH_INDEX_NAME, ctask.lookupIndex);
			Assert.assertEquals(ContributorService.SEARCH_INDEX_TYPE, ctask.lookupType);
			Assert.assertEquals("idtype", ctask.lookupField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals("myproject2", ctask.esValues[1]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}
	}

	@Test
	public void createTask_RENORMALIZE_BY_PROJECT_LOOKUP_ID() throws TaskConfigurationException, UnsupportedTaskException {
		ReindexingTaskFactory tested = getTested();

		// case - missing project code in configuration
		try {
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), null);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}
		// case - missing both config values in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			// OK
		}

		// case - missing CFG_PROJECT_ID_TYPE in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, "value");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_type configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, "value");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_type configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "   ");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, "value");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_type configuration property must be defined", e.getMessage());
		}

		// case - missing CFG_PROJECT_ID_VALUE in configuration
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, "  ");
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, new String[] {});
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, new String[] { " " });
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, new ArrayList<String>());
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_value configuration property must be defined", e.getMessage());
		}
		try {
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, Arrays.asList(new String[] { " ", "" }));
			tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.fail("TaskConfigurationException expected");
		} catch (TaskConfigurationException e) {
			Assert.assertEquals("project_id_value configuration property must be defined", e.getMessage());
		}

		// case - everything is OK
		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, "idvalue");
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsLookedUpValuesTask.class, task.getClass());
			RenormalizeByEsLookedUpValuesTask ctask = (RenormalizeByEsLookedUpValuesTask) task;
			Assert.assertEquals(ProjectService.SEARCH_INDEX_NAME, ctask.lookupIndex);
			Assert.assertEquals(ProjectService.SEARCH_INDEX_TYPE, ctask.lookupType);
			Assert.assertEquals("idtype", ctask.lookupField);
			Assert.assertEquals("idvalue", ctask.esValues[0]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, new String[] { "myproject", "myproject2" });
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsLookedUpValuesTask.class, task.getClass());
			RenormalizeByEsLookedUpValuesTask ctask = (RenormalizeByEsLookedUpValuesTask) task;
			Assert.assertEquals(ProjectService.SEARCH_INDEX_NAME, ctask.lookupIndex);
			Assert.assertEquals(ProjectService.SEARCH_INDEX_TYPE, ctask.lookupType);
			Assert.assertEquals("idtype", ctask.lookupField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals("myproject2", ctask.esValues[1]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}

		{
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_TYPE, "idtype");
			config.put(ReindexingTaskFactory.CFG_PROJECT_ID_VALUE, Arrays.asList(new String[] { "myproject", "myproject2" }));
			Task task = tested.createTask(ReindexingTaskTypes.RENORMALIZE_BY_PROJECT_LOOKUP_ID.getTaskType(), config);
			Assert.assertEquals(RenormalizeByEsLookedUpValuesTask.class, task.getClass());
			RenormalizeByEsLookedUpValuesTask ctask = (RenormalizeByEsLookedUpValuesTask) task;
			Assert.assertEquals(ProjectService.SEARCH_INDEX_NAME, ctask.lookupIndex);
			Assert.assertEquals(ProjectService.SEARCH_INDEX_TYPE, ctask.lookupType);
			Assert.assertEquals("idtype", ctask.lookupField);
			Assert.assertEquals("myproject", ctask.esValues[0]);
			Assert.assertEquals("myproject2", ctask.esValues[1]);
			Assert.assertEquals(tested.providerService, ctask.providerService);
			Assert.assertEquals(tested.searchClientService, ctask.searchClientService);
		}
	}

	private ReindexingTaskFactory getTested() {
		ReindexingTaskFactory tested = new ReindexingTaskFactory();
		tested.contentPersistenceService = Mockito.mock(ContentPersistenceService.class);
		tested.providerService = Mockito.mock(ProviderService.class);
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		return tested;
	}

}
