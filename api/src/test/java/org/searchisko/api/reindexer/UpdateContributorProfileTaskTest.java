/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.TaskExecutionContext;

/**
 * Unit test for {@link UpdateContributorProfileTask}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class UpdateContributorProfileTaskTest {

	@Test(expected = TaskConfigurationException.class)
	public void constructor_invalid_noconfig() throws TaskConfigurationException {
		new UpdateContributorProfileTask(Mockito.mock(ContributorProfileService.class), null);
	}

	@Test(expected = TaskConfigurationException.class)
	public void constructor_invalid_emptyconfig() throws TaskConfigurationException {
		new UpdateContributorProfileTask(Mockito.mock(ContributorProfileService.class), new HashMap<String, Object>());
	}

	@Test(expected = TaskConfigurationException.class)
	public void constructor_invalid_unsupportedtype() throws TaskConfigurationException {
		ContributorProfileService cpsmock = Mockito.mock(ContributorProfileService.class);
		Mockito.when(cpsmock.isContributorCodeTypesSupported("mytype")).thenReturn(false);

		Map<String, Object> cfg = new HashMap<>();
		cfg.put(UpdateContributorProfileTask.CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE, "mytype");

		new UpdateContributorProfileTask(cpsmock, cfg);
	}

	@Test
	public void constructor_valid_typeonly() throws TaskConfigurationException {
		ContributorProfileService cpsmock = Mockito.mock(ContributorProfileService.class);
		Mockito.when(cpsmock.isContributorCodeTypesSupported("mytype")).thenReturn(true);

		Map<String, Object> cfg = new HashMap<>();
		cfg.put(UpdateContributorProfileTask.CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE, "mytype");

		UpdateContributorProfileTask tested = new UpdateContributorProfileTask(cpsmock, cfg);

		Assert.assertEquals("mytype", tested.contributorCodeType);
		Assert.assertNull(tested.contributorCodeValues);
	}

	@Test
	public void constructor_valid_type_codes() throws TaskConfigurationException {
		ContributorProfileService cpsmock = Mockito.mock(ContributorProfileService.class);
		Mockito.when(cpsmock.isContributorCodeTypesSupported("mytype")).thenReturn(true);

		Map<String, Object> cfg = new HashMap<>();
		cfg.put(UpdateContributorProfileTask.CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE, "mytype");
		List<String> cv = new ArrayList<>();
		cv.add("quimby");
		cv.add("homers");
		cfg.put(UpdateContributorProfileTask.CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_VALUE, cv);

		UpdateContributorProfileTask tested = new UpdateContributorProfileTask(cpsmock, cfg);

		Assert.assertEquals("mytype", tested.contributorCodeType);
		Assert.assertArrayEquals(new String[] { "quimby", "homers" }, tested.contributorCodeValues);
	}

	@Test
	public void performTask() throws Exception {
		UpdateContributorProfileTask tested = getTested();
		tested.contributorCodeType = "cct";

		// case - reindex all
		tested.contributorCodeValues = null;

		tested.performTask();

		Mockito.verify(tested.contributorProfileService).createOrUpdateAllProfiles("cct");
		Mockito.verifyNoMoreInteractions(tested.contributorProfileService);

		// case - reindex named ones only
		Mockito.reset(tested.contributorProfileService);
		tested.contributorCodeValues = new String[] { "homers", "quimby" };

		tested.performTask();

		Mockito.verify(tested.contributorProfileService).createOrUpdateProfile("cct", "homers", true);
		Mockito.verify(tested.contributorProfileService).createOrUpdateProfile("cct", "quimby", true);
		Mockito.verifyNoMoreInteractions(tested.contributorProfileService);

	}

	private UpdateContributorProfileTask getTested() {
		UpdateContributorProfileTask ret = new UpdateContributorProfileTask();
		ret.contributorProfileService = Mockito.mock(ContributorProfileService.class);
		ret.setExecutionContext("tid", Mockito.mock(TaskExecutionContext.class));
		return ret;
	}

}
