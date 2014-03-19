/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.events;

import java.util.Map;

import org.searchisko.api.service.ContributorService;

/**
 * CDI Event emitted by {@link ContributorService} when some Contributor is updated. It is not emitted for Contributor
 * <code>code</code> change - {@link ContributorCodeChangedEvent} is emitted in this case. It is also not emitted for
 * contributors merge - {@link ContributorMergedEvent} is used in this case.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorUpdatedEvent {

	private String contributorId;

	private String contributorCode;

	private Map<String, Object> contributorData;

	/**
	 * @param contributorId identifier of contributor
	 * @param contributorCode 'code' of contributor
	 * @param contributorData with contributor data
	 */
	public ContributorUpdatedEvent(String contributorId, String contributorCode, Map<String, Object> contributorData) {
		super();
		this.contributorId = contributorId;
		this.contributorCode = contributorCode;
		this.contributorData = contributorData;
	}

	/**
	 * @return identifier of contributor
	 */
	public String getContributorId() {
		return contributorId;
	}

	/**
	 * @return 'code' of contributor
	 */
	public String getContributorCode() {
		return contributorCode;
	}

	/**
	 * @return contributor data
	 */
	public Map<String, Object> getContributorData() {
		return contributorData;
	}

	@Override
	public String toString() {
		return "ContributorUpdatedEvent [contributorId=" + contributorId + ", contributorCode=" + contributorCode
				+ ", contributorData=" + contributorData + "]";
	}

}
