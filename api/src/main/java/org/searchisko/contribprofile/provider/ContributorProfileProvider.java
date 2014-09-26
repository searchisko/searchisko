/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

import java.util.List;

import org.searchisko.contribprofile.model.ContributorProfile;

/**
 * Interface for provider of contributor profile
 *
 * @author Libor Krzyzanek
 */
public interface ContributorProfileProvider {

	static final String DCP_PROFILE_ACCOUNTS = "accounts";
	static final String DCP_PROFILE_ACCOUNT_DOMAIN = "domain";
	static final String DCP_PROFILE_ACCOUNT_USERNAME = "username";
	static final String DCP_PROFILE_ACCOUNT_LINK = "link";


	/**
	 * Get all profiles with pagination support
	 *
	 * @param start can be null to use default value
	 * @param size can be null to use default value
	 * @return List of ContributorProfile (can be empty) or null if something goes wrong
	 */
	public List<ContributorProfile> getAllProfiles(Integer start, Integer size);

	/**
	 * Retrieve contributor profile based on jboss.org username takene e.g. from CAS filters
	 *
	 * @param jbossorgUsername
	 * @return contributor profile or null if something goes wrong
	 */
	public ContributorProfile getProfile(String jbossorgUsername);

}
