/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.tasker.Task;
import org.searchisko.api.tasker.TaskConfigurationException;

/**
 * Task used to fully synchronize Contributors and its profiles from remote profile provider.
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.service.ContributorProfileService#isContributorCodeTypesSupported(String)
 */
public class FullSyncContributorAndProfilesTask extends Task {

	private Logger log = Logger.getLogger(FullSyncContributorAndProfilesTask.class.getName());

	public static final String CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE = "contributor_type_specific_code_type";
	public static final String CFG_START = "start";
	public static final String CFG_SIZE = "size";

	protected ContributorProfileService contributorProfileService;
	protected String contributorCodeType = null;
	protected Integer start;
	protected Integer size;

	public FullSyncContributorAndProfilesTask(ContributorProfileService contributorProfileService,
											  Map<String, Object> taskConfig) throws TaskConfigurationException {
		super();
		this.contributorProfileService = contributorProfileService;
		this.contributorCodeType = ReindexingTaskFactory.getMandatoryConfigString(taskConfig, CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE);

		this.start = ReindexingTaskFactory.getConfigInteger(taskConfig, CFG_START, false);
		this.size = ReindexingTaskFactory.getConfigInteger(taskConfig, CFG_SIZE, false);

		validateTaskConfiguration();
	}

	/**
	 * For unit tests.
	 */
	protected FullSyncContributorAndProfilesTask() {
	}

	protected void validateTaskConfiguration() throws TaskConfigurationException {
		if (!contributorProfileService.isContributorCodeTypesSupported(contributorCodeType)) {
			throw new TaskConfigurationException(CFG_CONTRIBUTOR_TYPE_SPECIFIC_CODE_TYPE + " is not supported");
		}
	}

	@Override
	public void performTask() throws Exception {
		writeTaskLog("Task started. Count of processed profiles: \n");
		int index = 0;
		if (start != null) {
			index = start;
		}
		int totalCount = 0;
		while (true) {
			int counter = contributorProfileService.fullSynContributorsAndProfiles(contributorCodeType, index, size);
			if (counter <= 0) {
				break;
			}
			index += counter;
			totalCount += counter;
			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Full Sync Status. index: {0}, total count: {1}",
						new Object[]{index, totalCount});
			}
			// TODO: Put progress to new field: https://github.com/searchisko/searchisko/issues/155
			//writeTaskLog(totalCount + ";");
		}
		writeTaskLog("\nTask Finished. Total count of processed profiles: " + totalCount);
	}
}
