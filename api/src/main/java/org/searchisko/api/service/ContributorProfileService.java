/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.contribprofile.provider.Jive6ContributorProfileProvider;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for handling contributor profiles
 *
 * @author Libor Krzyzanek
 */
@Named
@Stateless
@LocalBean
public class ContributorProfileService {

	@Inject
	protected Logger log;

	@Inject
	protected Jive6ContributorProfileProvider contributorProfileProvider;

	public String getContributorId(String username) {
		// TODO: Search ContributorProfile in index just for contributor Id based on jboss.org username
		return contributorProfileProvider.getProfile(username).getContributorId();
	}

	public void createOrUpdateProfile(String username) {
		log.log(Level.FINE, "Create or update profile for username {0}", username);

		// TODO: call this method when user successfully log in.

		ContributorProfile profile = contributorProfileProvider.getProfile(username);
		// TODO: Insert contributor profile into index
	}

}
