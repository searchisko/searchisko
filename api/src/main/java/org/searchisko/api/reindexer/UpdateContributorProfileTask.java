/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import org.searchisko.api.service.ContributorProfileService;
import org.searchisko.api.tasker.Task;

/**
 * Task used to update Contributor profiles from remote profile provider.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see ContributorProfileService#createOrUpdateAllProfiles(String);
 */
public class UpdateContributorProfileTask extends Task {

	protected ContributorProfileService contributorProfileService;

	public UpdateContributorProfileTask(ContributorProfileService contributorProfileService) {
		super();
		this.contributorProfileService = contributorProfileService;
	}

	@Override
	public void performTask() throws Exception {
		contributorProfileService.createOrUpdateAllProfiles(ContributorProfileService.FIELD_TSC_JBOSSORG_USERNAME);
	}
}
