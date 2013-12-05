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
	 * jboss.org username
	 */
	private String jbossorgUsername;

	/**
	 * Jive profile data
	 */
	private Map<String, Object> jiveProfile;

	public ContributorProfile(String contributorId, String jbossorgUsername) {
		this.contributorId = contributorId;
		this.jbossorgUsername = jbossorgUsername;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ContributorProfile that = (ContributorProfile) o;

		if (!contributorId.equals(that.contributorId)) return false;
		if (!jbossorgUsername.equals(that.jbossorgUsername)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = contributorId.hashCode();
		result = 31 * result + jbossorgUsername.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ContributorProfile{" +
				"contributorId='" + contributorId + '\'' +
				", jbossorgUsername='" + jbossorgUsername + '\'' +
				", jiveProfile=" + jiveProfile +
				'}';
	}

	public String getContributorId() {
		return contributorId;
	}

	public void setContributorId(String contributorId) {
		this.contributorId = contributorId;
	}

	public String getJbossorgUsername() {
		return jbossorgUsername;
	}

	public void setJbossorgUsername(String jbossorgUsername) {
		this.jbossorgUsername = jbossorgUsername;
	}

	public Map<String, Object> getJiveProfile() {
		return jiveProfile;
	}

	public void setJiveProfile(Map<String, Object> jiveProfile) {
		this.jiveProfile = jiveProfile;
	}
}
