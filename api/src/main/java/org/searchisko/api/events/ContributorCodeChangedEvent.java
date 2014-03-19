/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.events;

import org.searchisko.api.service.ContributorService;

/**
 * CDI Event emitted by {@link ContributorService} when Contributor's <code>code</code> is changed.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorCodeChangedEvent {

	private String contributorCodeFrom;

	private String contributorCodeTo;

	/**
	 * @param contributorCodeFrom old 'code' of contributor
	 * @param contributorCodeTo new 'code' of contributor
	 */
	public ContributorCodeChangedEvent(String contributorCodeFrom, String contributorCodeTo) {
		super();
		this.contributorCodeFrom = contributorCodeFrom;
		this.contributorCodeTo = contributorCodeTo;
	}

	/**
	 * @return old 'code' of contributor
	 */
	public String getContributorCodeFrom() {
		return contributorCodeFrom;
	}

	/**
	 * @return new 'code' of contributor
	 */
	public String getContributorCodeTo() {
		return contributorCodeTo;
	}

	@Override
	public String toString() {
		return "ContributorCodeChangedEvent [contributorCodeFrom=" + contributorCodeFrom + ", contributorCodeTo="
				+ contributorCodeTo + "]";
	}

}
