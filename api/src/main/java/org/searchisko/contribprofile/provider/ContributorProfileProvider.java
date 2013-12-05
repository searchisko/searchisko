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

	/**
	 * Retrieve contributor profile based on jboss.org username takene e.g. from CAS filters
	 *
	 * @param jbossorgUsername
	 * @return
	 */
	public ContributorProfile getProfile(String jbossorgUsername);

}
