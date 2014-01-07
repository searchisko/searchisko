/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.reindexer.ReindexingTaskFactory;
import org.searchisko.api.reindexer.ReindexingTaskTypes;
import org.searchisko.api.rest.security.ContributorAuthenticationInterceptor;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.UnsupportedTaskException;
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

	@Inject
	protected TaskService taskService;

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

			String contributorId = createOrUpdateContributorRecord(profile, FIELD_TSC_JBOSSORG_USERNAME, username);

			// TODO CONTRIBUTOR_PROFILE create and insert contributor_profile document into search index

			return contributorId;
		} else {
			throw new IllegalArgumentException("Usernames from " + authenticationScheme + " are not supported");
		}

	}

	protected String createOrUpdateContributorRecord(ContributorProfile profile, String typeSpecificCodeField,
			String typeSpecificCode) {

		List<String> toRenormalizeContributorIds = new ArrayList<>();

		String ci = ContributorService.createContributorId(profile.getFullName(), profile.getPrimaryEmail());

		SearchHit contributorById = null;
		SearchResponse srById = contributorService.findByCode(ci);
		if (srById.getHits().getTotalHits() > 0) {
			if (srById.getHits().getTotalHits() > 1) {
				log.warning("Contributor configuration problem! We found more Contributor definitions for code=" + ci
						+ ". For now we use first one, but problem should be resolved by administrator!");
			}
			contributorById = srById.getHits().getHits()[0];
		}

		SearchHit contributorByTsc = null;
		SearchResponse srByTsc = contributorService.findByTypeSpecificCode(typeSpecificCodeField, typeSpecificCode);
		if (srByTsc.getHits().getTotalHits() > 0) {
			if (srByTsc.getHits().getTotalHits() > 1) {
				// TODO CONTRIBUTOR_PROFILE try to find correct one by ci. Ten we should remove typeSpecificCode from others and
				// add them into reindx task (toRenormalizeContributorIds).
				log.warning("Contributor configuration problem! We found more Contributor definitions for "
						+ ContributorService.FIELD_TYPE_SPECIFIC_CODE + "/" + typeSpecificCodeField + "=" + typeSpecificCode
						+ ". For now we use first one, but problem should be resolved by administrator!");
			}
			contributorByTsc = srByTsc.getHits().getHits()[0];
			if (SearchUtils.isBlank((String) contributorByTsc.getSource().get(ContributorService.FIELD_CODE))) {
				String msg = "Contributor configuration problem! 'code' field is empty for Contributor with id="
						+ contributorByTsc.getId() + " so we can't use it. Skipping this record for now.";
				log.log(Level.WARNING, msg);
				contributorByTsc = null;
			}
		}

		String contributorEntityId = null;
		Map<String, Object> contributorEntityContent = null;
		if (contributorById != null && contributorByTsc != null) {
			contributorEntityId = contributorById.getId();
			contributorEntityContent = contributorById.getSource();
			String ciFromTsc = (String) contributorByTsc.getSource().get(ContributorService.FIELD_CODE);
			if (!ci.equals(ciFromTsc)) {
				log.info("Contributor duplicity detected. We are going to merge contributor '" + ciFromTsc
						+ "' into contributor '" + ci + "'");
				toRenormalizeContributorIds.add(ciFromTsc);
				// TODO CONTRIBUTOR_PROFILE merge data from contributorByTsc into contributorEntityContent
				contributorService.delete(contributorByTsc.getId());
			}
		} else if (contributorById == null && contributorByTsc != null) {
			ci = (String) contributorByTsc.getSource().get(ContributorService.FIELD_CODE);
			contributorEntityId = contributorByTsc.getId();
			contributorEntityContent = contributorByTsc.getSource();
		} else if (contributorById != null && contributorByTsc == null) {
			contributorEntityId = contributorById.getId();
			contributorEntityContent = contributorById.getSource();
		} else {
			// TODO CONTRIBUTOR_PROFILE try to find existing Contributor by any of email addresses available in profile
			contributorEntityContent = new HashMap<String, Object>();
		}

		contributorEntityContent.put(ContributorService.FIELD_CODE, ci);
		// TODO CONTRIBUTOR_PROFILE upgrade contributorEntityContent with new values
		// TODO CONTRIBUTOR_PROFILE check email uniqueness (remove them from other Contributor records if any)

		if (contributorEntityId != null)
			contributorService.create(contributorEntityId, contributorEntityContent);
		else
			contributorService.create(contributorEntityContent);

		if (!toRenormalizeContributorIds.isEmpty()) {
			if (log.isLoggable(Level.FINE))
				log.fine("We are going to renormalize content for contributor codes: " + toRenormalizeContributorIds);
			Map<String, Object> taskConfig = new HashMap<String, Object>();
			taskConfig.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, toRenormalizeContributorIds);
			try {
				taskService.getTaskManager().createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(),
						taskConfig);
			} catch (UnsupportedTaskException | TaskConfigurationException e) {
				log.severe("Problem to start contributor renormalization task: " + e.getMessage());
			}
		}

		return ci;

	}
}
