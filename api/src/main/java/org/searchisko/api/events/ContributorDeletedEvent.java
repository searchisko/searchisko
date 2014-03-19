/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.events;

import org.searchisko.api.service.ContributorService;

/**
 * CDI Event emitted by {@link ContributorService#delete(String)} when some Contributor is deleted. This event is not
 * fired if Contributor is deleted due merge with another contributor, {@link ContributorMergedEvent} is fired in this
 * case!
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorDeletedEvent {

	private String contributorId;

	private String contributorCode;

	/**
	 * @param contributorId identifier of deleted contributor
	 * @param contributorCode 'code' of deleted contributor
	 */
	public ContributorDeletedEvent(String contributorId, String contributorCode) {
		super();
		this.contributorId = contributorId;
		this.contributorCode = contributorCode;
	}

	/**
	 * @return identifier of deleted contributor
	 */
	public String getContributorId() {
		return contributorId;
	}

	/**
	 * @return 'code' of deleted contributor
	 */
	public String getContributorCode() {
		return contributorCode;
	}

	@Override
	public String toString() {
		return "ContributorDeletedEvent [contributorId=" + contributorId + ", contributorCode=" + contributorCode + "]";
	}

}
