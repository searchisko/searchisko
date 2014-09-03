/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.contribprofile.provider;

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
	 * Retrieve contributor profile based on jboss.org username takene e.g. from CAS filters
	 * 
	 * @param jbossorgUsername
	 * @return
	 */
	public ContributorProfile getProfile(String jbossorgUsername);

}
