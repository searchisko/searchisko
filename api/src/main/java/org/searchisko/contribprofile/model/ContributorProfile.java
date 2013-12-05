/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.model;

import java.util.Map;

/**
 * Model object for Contributor Profile
 *
 * @author Libor Krzyzanek
 */
public class ContributorProfile {

	/**
	 * Contributor ID
	 */
	private String contributorId;

	/**
	 * Free profile data
	 */
	private Map<String, Object> profile;

	public ContributorProfile(String contributorId) {
		this.contributorId = contributorId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ContributorProfile that = (ContributorProfile) o;

		if (!contributorId.equals(that.contributorId)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return contributorId.hashCode();
	}

	@Override
	public String toString() {
		return "ContributorProfile{" +
				"contributorId='" + contributorId + '\'' +
				", profile=" + profile +
				'}';
	}

	public String getContributorId() {
		return contributorId;
	}

	public void setContributorId(String contributorId) {
		this.contributorId = contributorId;
	}

	public Map<String, Object> getProfile() {
		return profile;
	}

	public void setProfile(Map<String, Object> profile) {
		this.profile = profile;
	}
}
