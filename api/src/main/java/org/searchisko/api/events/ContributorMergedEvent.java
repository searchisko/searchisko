/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.events;

import org.searchisko.api.service.ContributorService;

/**
 * CDI Event emitted by {@link ContributorService} when two Contributors are merged into one. Contains <code>code</code>
 * s of both contributors.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorMergedEvent {

	private String contributorCodeFrom;

	private String contributorCodeTo;

	/**
	 * @param contributorCodeFrom code of contributor who has been merged, so do not exists anymore
	 * @param contributorCodeTo code of contributor who is target of merge, so exists
	 */
	public ContributorMergedEvent(String contributorCodeFrom, String contributorCodeTo) {
		super();
		this.contributorCodeFrom = contributorCodeFrom;
		this.contributorCodeTo = contributorCodeTo;
	}

	/**
	 * @return 'code' of contributor who has been merged (so not present anymore)
	 */
	public String getContributorCodeFrom() {
		return contributorCodeFrom;
	}

	/**
	 * @return 'code' of contributor who is target of merge (so is present)
	 */
	public String getContributorCodeTo() {
		return contributorCodeTo;
	}

	@Override
	public String toString() {
		return "ContributorMergedEvent [contributorCodeFrom=" + contributorCodeFrom + ", contributorCodeTo="
				+ contributorCodeTo + "]";
	}

}
