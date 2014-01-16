/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.rest.security.ContributorAuthenticationInterceptor;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.contribprofile.provider.Jive6ContributorProfileProvider;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for handling 'Contributor profiles'.
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Named
@ApplicationScoped
@Singleton
public class ContributorProfileService {

	public static final String FIELD_TSC_JBOSSORG_USERNAME = "jbossorg_username";
	public static final String FIELD_TSC_GITHUB_USERNAME = "github_username";

	public static final String SEARCH_INDEX_NAME = "data_contributor_profile";

	public static final String SEARCH_INDEX_TYPE = "jbossorg_contributor_profile";

	@Inject
	protected Logger log;

	@Inject
	protected Jive6ContributorProfileProvider contributorProfileProvider;

	@Inject
	protected ContributorService contributorService;

	@Inject
	protected SearchClientService searchClientService;

	/**
	 * Updates search index by current entity identified by id
	 *
	 * @param id
	 * @param entity
	 */
	private void updateSearchIndex(String id, Map<String, Object> entity) {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Updating profile, id: {0}, data: {1}", new Object[]{id, entity});
		}
		searchClientService.performPut(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id, entity);
		searchClientService.performIndexFlushAndRefresh(SEARCH_INDEX_NAME);
	}

	/**
	 * Put entity to search index
	 *
	 * @param entity
	 */
	private void putToSearchIndex(Map<String, Object> entity) {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Updating profile, data: {0}", entity);
		}
		searchClientService.performPut(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, entity);
		searchClientService.performIndexFlushAndRefresh(SEARCH_INDEX_NAME);
	}

	/**
	 * Get contributor id based on username and authentication method.
	 *
	 * @param authenticationScheme we pass username for. This allows to use more authentication methods for contributors
	 *                             to be used in same time. You can see {@link ContributorAuthenticationInterceptor} for possible values.
	 * @param username             of contributor in passed in <code>authenticationScheme</code> to get profile for
	 * @param forceCreate          if <code>true</code> we need contributor id so backend should create it for logged in user if
	 *                             not created yet. If <code>false</code> then we do not need it currently, so system can't create it but
	 *                             return null instead.
	 * @return contributor id - can be null if <code><forceCreate</code> is false and contributor record do not exists yet
	 * for current user.
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
	 *                             to be used in same time. You can see {@link ContributorAuthenticationInterceptor} for possible values.
	 * @param username             of contributor in passed in <code>authenticationScheme</code> to get profile for
	 * @return contributor code
	 */
	public String createOrUpdateProfile(String authenticationScheme, String username) {
		log.log(Level.FINE, "Create or update profile for username {0}", username);

		if (ContributorAuthenticationInterceptor.AUTH_METHOD_CAS.equals(authenticationScheme)) {

			// TODO CONTRIBUTOR_PROFILE we call this method when user successfully authenticate which may be rather often. So
			// perform profile update only once a time (store last update timestamp in profile's field sys_updated and use it).

			ContributorProfile profile = contributorProfileProvider.getProfile(username);
			if (profile == null) {
				log.log(Level.WARNING, "User not found in contributor profile provider for jboss.org username: {0}", username);
				return null;
			}

			String contributorCode = contributorService.createOrUpdateFromProfile(profile, FIELD_TSC_JBOSSORG_USERNAME,
					username);

			Map<String, Object> profileData = profile.getProfileData();
			List<String> contributors = new ArrayList<>(1);
			contributors.add(contributorCode);

			profileData.put(ContentObjectFields.SYS_CONTRIBUTORS, contributors);

			// Search profiles with same sys_contributors and update them.
			SearchResponse matchingProfiles = findByContributorCode(contributorCode);
			if (matchingProfiles != null && matchingProfiles.getHits().getTotalHits() > 0) {
				if (matchingProfiles.getHits().getTotalHits() != 1) {
					log.log(Level.SEVERE, "Data inconsistency: Contributor has more than one profile in search index. " +
							"Skipping updating them. Contributor code: {0}", contributorCode);
				} else {
					for (SearchHit p : matchingProfiles.getHits().getHits()) {
						updateSearchIndex(p.getId(), profileData);
					}
				}
			} else {
				putToSearchIndex(profileData);
			}


			return contributorCode;
		} else {
			throw new IllegalArgumentException("Username from " + authenticationScheme + " are not supported");
		}

	}

	public SearchResponse findByContributorCode(String contributorCode) {
		try {
			return searchClientService.performFilterByOneField(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, ContentObjectFields.SYS_CONTRIBUTORS, contributorCode);
		} catch (IndexMissingException e) {
			return null;
		}
	}

	public void deleteByContributorCode(String code) {
		SearchResponse matchingProfiles = findByContributorCode(code);
		if (matchingProfiles != null && matchingProfiles.getHits().getTotalHits() > 0) {
			for (SearchHit p : matchingProfiles.getHits().getHits()) {
				searchClientService.performDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, p.getId());
			}
		}
	}

}
