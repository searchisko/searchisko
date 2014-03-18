/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.Map;

import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.tasker.Task;
import org.searchisko.api.tasker.TaskConfigurationException;

/**
 * Task used to update Contributor profiles from remote profile provider.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see ContributorProfileService#createOrUpdateAllProfiles(String);
 */
public class UpdateContributorProfileTask extends Task {

	public static final String CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE = "contributor_type_specific_code_type";
	public static final String CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_VALUE = "contributor_type_specific_code_value";

	protected ContributorProfileService contributorProfileService;
	protected String contributorCodeType = null;
	protected String[] contributorCodeValues = null;

	public UpdateContributorProfileTask(ContributorProfileService contributorProfileService,
			Map<String, Object> taskConfig) throws TaskConfigurationException {
		super();
		this.contributorProfileService = contributorProfileService;
		this.contributorCodeType = ReindexingTaskFactory.getMandatoryConfigString(taskConfig,
				CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE);
		this.contributorCodeValues = ReindexingTaskFactory.getConfigStringArray(taskConfig,
				CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_VALUE);
		validateTaskConfiguration();
	}

	/**
	 * For unit tests.
	 */
	protected UpdateContributorProfileTask() {

	}

	protected void validateTaskConfiguration() throws TaskConfigurationException {
		if (!contributorProfileService.isContributorCodeTypesSupported(contributorCodeType)) {
			throw new TaskConfigurationException(CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE + " is not supported");
		}
	}

	@Override
	public void performTask() throws Exception {
		int i = 0;
		if (contributorCodeValues == null) {
			i = contributorProfileService.createOrUpdateAllProfiles(contributorCodeType);
		} else {
			for (String v : contributorCodeValues) {
				i++;
				contributorProfileService.createOrUpdateProfile(contributorCodeType, v, true);
			}
		}
		writeTaskLog(i + " contributor profiles processed.");
	}
}
