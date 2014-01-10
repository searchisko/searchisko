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

import org.elasticsearch.action.search.SearchResponse;
import org.searchisko.api.rest.security.ContributorAuthenticationInterceptor;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.contribprofile.provider.Jive6ContributorProfileProvider;

/**
 * Service for handling 'Contributor profiles'.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@Stateless
@LocalBean
public class ContributorProfileService {

	public static final String FIELD_TSC_JBOSSORG_USERNAME = "jbossorg_username";
	public static final String FIELD_TSC_GITHUB_USERNAME = "github_username";

	@Inject
	protected Logger log;

	@Inject
	protected Jive6ContributorProfileProvider contributorProfileProvider;

	@Inject
	protected ContributorService contributorService;

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

			SearchResponse sr = contributorService.findByTypeSpecificCode(FIELD_TSC_JBOSSORG_USERNAME, username);
			if (sr.getHits().getTotalHits() > 0) {
				if (sr.getHits().getTotalHits() > 1) {
					log.warning("Contributor configuration problem! We found more Contributor definitions for jbossorg_username="
							+ username + ". For now we use first one, but problem should be resolved by administrator!");
				}
				String c = (String) sr.getHits().getHits()[0].getSource().get(ContributorService.FIELD_CODE);
				if (SearchUtils.isBlank(c)) {
					String msg = "Contributor configuration problem! 'code' field is empty for contributor id="
							+ sr.getHits().getHits()[0].getId();
					log.log(Level.WARNING, msg);
					throw new IllegalArgumentException(msg);
				}
				return c;
			}

			if (!forceCreate) {
				return null;
			}

			return createOrUpdateProfile(authenticationScheme, username);
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
	 * @return contributor id
	 */
	public String createOrUpdateProfile(String authenticationScheme, String username) {
		log.log(Level.FINE, "Create or update profile for username {0}", username);
		if (ContributorAuthenticationInterceptor.AUTH_METHOD_CAS.equals(authenticationScheme)) {

			// TODO CONTRIBUTOR_PROFILE we call this method when user successfully authenticate which may be rather often. So
			// perform profile update only once a time (store last update timestamp in profile and use it).

			ContributorProfile profile = contributorProfileProvider.getProfile(username);
			if (profile == null) {
				log.warning("User not found in contributor profile provider for jboss.org username: " + username);
				return null;
			}

			String contributorId = contributorService.createOrUpdateFromProfile(profile, FIELD_TSC_JBOSSORG_USERNAME,
					username);

			// TODO CONTRIBUTOR_PROFILE create and insert contributor_profile document into search index

			return contributorId;
		} else {
			throw new IllegalArgumentException("Usernames from " + authenticationScheme + " are not supported");
		}

	}

	public void deleteByContributorCode(String code) {
		// TODO CONTRIBUTOR_PROFILE implement profile deletion
	}

}
