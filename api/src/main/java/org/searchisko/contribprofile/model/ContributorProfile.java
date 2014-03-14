/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.model;

import java.util.List;
import java.util.Map;

import org.searchisko.api.util.SearchUtils;

/**
 * Model object for Contributor Profile data readed from remote profile provider.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorProfile {

	private String id;

	private String primaryEmail;

	private String fullName;

	private List<String> emails;

	private Map<String, List<String>> typeSpecificCodes;

	/**
	 * <code>contributor_profile</code> JSON data structure.
	 */
	private Map<String, Object> profileData;

	public ContributorProfile(String id, String fullName, String primaryEmail, List<String> emails,
			Map<String, List<String>> typeSpecificCodes) {
		super();
		id = SearchUtils.trimToNull(id);
		fullName = SearchUtils.trimToNull(fullName);
		primaryEmail = SearchUtils.trimToNull(primaryEmail);
		if (id == null || fullName == null || primaryEmail == null)
			throw new IllegalArgumentException("id and fullName and primaryEmail can't be null or empty");
		if (emails == null || typeSpecificCodes == null)
			throw new IllegalArgumentException("emails and typeSpecificCodes can't be null");
		this.id = id;
		this.fullName = fullName;
		this.primaryEmail = primaryEmail;
		this.emails = emails;
		this.typeSpecificCodes = typeSpecificCodes;
	}

	public Map<String, Object> getProfileData() {
		return profileData;
	}

	public void setProfileData(Map<String, Object> profileData) {
		this.profileData = profileData;
	}

	public String getPrimaryEmail() {
		return primaryEmail;
	}

	public String getFullName() {
		return fullName;
	}

	public List<String> getEmails() {
		return emails;
	}

	public Map<String, List<String>> getTypeSpecificCodes() {
		return typeSpecificCodes;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "ContributorProfile [id=" + id + ",primaryEmail=" + primaryEmail + ", fullName=" + fullName + ", emails="
				+ emails + ", typeSpecificCodes=" + typeSpecificCodes + ", profileData=" + profileData + "]";
	}

}
