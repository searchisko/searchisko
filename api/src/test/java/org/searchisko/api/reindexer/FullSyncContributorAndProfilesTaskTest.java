/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.reindexer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.TaskExecutionContext;

/**
 * Unit test for {@link org.searchisko.api.reindexer.FullSyncContributorAndProfilesTask}
 *
 * @author Libor Krzyzanek
 */
public class FullSyncContributorAndProfilesTaskTest {

	@Test(expected = TaskConfigurationException.class)
	public void constructor_invalid_noconfig() throws TaskConfigurationException {
		new FullSyncContributorAndProfilesTask(Mockito.mock(ContributorProfileService.class), null);
	}

	@Test(expected = TaskConfigurationException.class)
	public void constructor_invalid_emptyconfig() throws TaskConfigurationException {
		new FullSyncContributorAndProfilesTask(Mockito.mock(ContributorProfileService.class), new HashMap<String, Object>());
	}

	@Test(expected = TaskConfigurationException.class)
	public void constructor_invalid_unsupportedtype() throws TaskConfigurationException {
		ContributorProfileService cpsmock = Mockito.mock(ContributorProfileService.class);
		Mockito.when(cpsmock.isContributorCodeTypesSupported("mytype")).thenReturn(false);

		Map<String, Object> cfg = new HashMap<>();
		cfg.put(FullSyncContributorAndProfilesTask.CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE, "mytype");

		new FullSyncContributorAndProfilesTask(cpsmock, cfg);
	}

	@Test
	public void constructor_valid_typeonly() throws TaskConfigurationException {
		ContributorProfileService cpsmock = Mockito.mock(ContributorProfileService.class);
		Mockito.when(cpsmock.isContributorCodeTypesSupported("mytype")).thenReturn(true);

		Map<String, Object> cfg = new HashMap<>();
		cfg.put(FullSyncContributorAndProfilesTask.CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE, "mytype");

		FullSyncContributorAndProfilesTask tested = new FullSyncContributorAndProfilesTask(cpsmock, cfg);

		Assert.assertEquals("mytype", tested.contributorCodeType);
		Assert.assertNull(tested.start);
		Assert.assertNull(tested.size);
	}

	@Test
	public void constructor_valid_all_params() throws TaskConfigurationException {
		ContributorProfileService cpsmock = Mockito.mock(ContributorProfileService.class);
		Mockito.when(cpsmock.isContributorCodeTypesSupported("mytype")).thenReturn(true);

		Map<String, Object> cfg = new HashMap<>();
		cfg.put(FullSyncContributorAndProfilesTask.CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE, "mytype");
		cfg.put(FullSyncContributorAndProfilesTask.CFG_START, 100);
		cfg.put(FullSyncContributorAndProfilesTask.CFG_SIZE, 50);

		FullSyncContributorAndProfilesTask tested = new FullSyncContributorAndProfilesTask(cpsmock, cfg);

		Assert.assertEquals("mytype", tested.contributorCodeType);
		Assert.assertEquals(new Integer(100), tested.start);
		Assert.assertEquals(new Integer(50), tested.size);
	}

	@Test
	public void performTask() throws Exception {
		FullSyncContributorAndProfilesTask tested = getTested();
		tested.contributorCodeType = "cct";

		tested.performTask();
		Mockito.when(tested.contributorProfileService.fullSynContributorsAndProfiles(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(0);

		// case - no start, size
		Mockito.verify(tested.contributorProfileService).fullSynContributorsAndProfiles("cct", new Integer(0), null);
		Mockito.verifyNoMoreInteractions(tested.contributorProfileService);

		// case - start, size present
		Mockito.reset(tested.contributorProfileService);
		tested.start = 100;
		tested.size = 50;

		tested.performTask();

		Mockito.verify(tested.contributorProfileService).fullSynContributorsAndProfiles("cct", new Integer(100), new Integer(50));
		Mockito.verifyNoMoreInteractions(tested.contributorProfileService);

	}

	private FullSyncContributorAndProfilesTask getTested() {
		FullSyncContributorAndProfilesTask ret = new FullSyncContributorAndProfilesTask();
		ret.contributorProfileService = Mockito.mock(ContributorProfileService.class);
		ret.setExecutionContext("tid", Mockito.mock(TaskExecutionContext.class));
		return ret;
	}

}
