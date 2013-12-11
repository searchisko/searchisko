/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.searchisko.api.rest.security.ContributorAuthenticationInterceptor;
import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.contribprofile.provider.Jive6ContributorProfileProvider;

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

	/**
	 * Get contributor id based on username and authentication method.
	 * 
	 * @param authenticationScheme we pass username for. This allows to use more authentication methods for contributors
	 *          to be used in same time. You can see {@link ContributorAuthenticationInterceptor} for possible values.
	 * @param username of contributor in passed in <code>authenticationScheme</code> to get profile for
	 * @param forceCreate if <code>true</code> we need contributor id so backend should create it for logged in user if
	 *          not created yet. If <code>false</code> then we do not need it currently, so system can't create it but
	 *          return null instead.
	 * @return contributor id - can be null if <code><forceCreate</code> is false and contributor record do not exists yet
	 *         for current user.
	 */
	public String getContributorId(String authenticationScheme, String username, boolean forceCreate) {
		if (ContributorAuthenticationInterceptor.AUTH_METHOD_CAS.equals(authenticationScheme)) {
			// TODO CONTRIBUTOR_PROFILE Search ContributorProfile in index just for contributor Id based on jboss.org
			// username, use forceCreate here
			return contributorProfileProvider.getProfile(username).getContributorId();
		} else {
			throw new IllegalArgumentException("Usernames from " + authenticationScheme + " are not supported");
		}
	}

	/**
	 * Create or Update contributor profile based on username and authentication method.
	 * 
	 * @param authenticationScheme we pass username for. This allows to use more authentication methods for contributors
	 *          to be used in same time. You can see {@link ContributorAuthenticationInterceptor} for possible values.
	 * @param username of contributor in passed in <code>authenticationScheme</code> to get profile for
	 */
	public void createOrUpdateProfile(String authenticationScheme, String username) {
		log.log(Level.FINE, "Create or update profile for username {0}", username);
		if (ContributorAuthenticationInterceptor.AUTH_METHOD_CAS.equals(authenticationScheme)) {

			// TODO CONTRIBUTOR_PROFILE we call this method when user successfully authenticate which may be rather often. So
			// perform profile update only once a time (store last update timestamp in profile and use it).

			ContributorProfile profile = contributorProfileProvider.getProfile(username);
			// TODO CONTRIBUTOR_PROFILE Insert contributor profile into index
		} else {
			throw new IllegalArgumentException("Usernames from " + authenticationScheme + " are not supported");
		}

	}

}
