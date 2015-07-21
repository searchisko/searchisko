/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Singleton;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.events.ContributorCodeChangedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.api.events.ContributorMergedEvent;
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
@ApplicationScoped
@Singleton
@Lock(LockType.READ)
public class ContributorProfileService {

	/** Type specific code for jboss.org username */
	public static final String FIELD_TSC_JBOSSORG_USERNAME = "jbossorg_username";
	/** Type specific code for github.com username */
	public static final String FIELD_TSC_GITHUB_USERNAME = "github_username";

	private static final Set<String> TSC_SUPPORTED = new HashSet<>();
	static {
		TSC_SUPPORTED.add(FIELD_TSC_JBOSSORG_USERNAME);
	}

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

	@Inject
	protected AppConfigurationService appConfigurationService;

	/**
	 * Updates search index by current entity identified by id
	 *
	 * @param id of entity to update
	 * @param entity to update
	 */
	protected void updateSearchIndex(String id, Map<String, Object> entity) {
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "Updating profile, id: {0}, data: {1}", new Object[] { id, entity });
		}
		searchClientService.performPut(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id, entity);
		searchClientService.performIndexFlushAndRefresh(SEARCH_INDEX_NAME);
	}

	/**
	 * Check if Contributor type specific codes is supported by this service - we have contributor profile provider for it
	 * available, so it can be passed into other methods in this service which require external provider.
	 * <p>
	 * // TODO CONTRIBUTOR_PROFILE support for more profile providers, eg. for distinct contributorCodeType
	 *
	 * @param contributorCodeType of contributor "Type Specific Code" (eg. jboss.org username, github username etc, see
	 *          <code>FIELD_TSC_xx</code> constants) to check
	 * @return true if given type is supported - we can handle profile update for it.
	 */
	public boolean isContributorCodeTypesSupported(String contributorCodeType) {
		return TSC_SUPPORTED.contains(contributorCodeType);
	}

	/**
	 * Get contributor id based on contributor's "Type Specific Code", eg. obtained from authentication.
	 *
	 * @param contributorCodeType type of contributor's "Type Specific Code" (eg. jboss.org username, github username etc,
	 *          see <code>FIELD_TSC_xx</code> constants)
	 * @param contributorCodeValue code value to get contributor <code>code</code> for.
	 * @param forceCreate if <code>true</code> we need contributor id so backend should create it for logged in user if
	 *          not created yet. If <code>false</code> then we do not need it currently, so system can't create it but
	 *          return null instead.
	 * @return contributor identifier (<code>code</code> field) - can be null if <code><forceCreate</code> is false and
	 *         contributor record do not exists yet for current user.
	 * @throws RuntimeException if forceCreate is true but we are not able to obtain contributor identifier (for example
	 *           external Contributor Profile provider is not available just now)
	 */
	public String getContributorId(String contributorCodeType, String contributorCodeValue, boolean forceCreate) {

		SearchResponse sr = contributorService.findByTypeSpecificCode(contributorCodeType, contributorCodeValue);
		if (sr.getHits().getTotalHits() > 0) {
			if (sr.getHits().getTotalHits() > 1) {
				log.warning("Contributor configuration problem! We found more Contributor definitions for "
						+ contributorCodeType + "=" + contributorCodeValue
						+ ". For now we use first one, but problem should be resolved by administrator!");
			}
			String c = ContributorService.getContributorCode(sr.getHits().getHits()[0].getSource());
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

		String ret = createOrUpdateProfile(contributorCodeType, contributorCodeValue, false);

		if (ret == null && forceCreate) {
			throw new RuntimeException("Contributor record required but we are not able to create it just now.");
		}

		return ret;
	}

	/**
	 * Create or Update contributor profile based on Contributor's "Type Specific Code".
	 *
	 * @param contributorCodeType type of contributor "Type Specific Code" (eg. jboss.org username, github username etc,
	 *          see <code>FIELD_TSC_xx</code> constants)
	 * @param contributorCodeValue of code to get create or update profile for.
	 * @param forceUpdate if true profile is updated always. If false then last update date is consulted and update
	 *          performed only for old profiles.
	 * @return Contributor's <code>code</code>
	 */
	public String createOrUpdateProfile(String contributorCodeType, String contributorCodeValue, boolean forceUpdate) {
		log.log(Level.FINE, "Create or update profile for username {0}", contributorCodeValue);

		String contributorCode = null;
		if (forceUpdate) {
			int thresholdInMinutes = appConfigurationService.getAppConfiguration().getContributorProfileUpdateThreshold();
			// Get matching contributor profile and check when it was updated
			SearchResponse currentContributors = contributorService.findByTypeSpecificCode(contributorCodeType,
					contributorCodeValue);

			if (currentContributors != null && currentContributors.getHits().getTotalHits() > 0) {
				SearchHit contributor = currentContributors.getHits().getAt(0);
				contributorCode = ContributorService.getContributorCode(contributor.getSource());

				SearchResponse currentProfiles = findByContributorCode(contributorCode);
				if (currentProfiles != null && currentProfiles.getHits().getTotalHits() > 0) {
					SearchHit profile = currentProfiles.getHits().getAt(0);
					Object updated = profile.getSource().get(ContentObjectFields.SYS_UPDATED);
					if (SearchUtils.isDateAfter(updated, thresholdInMinutes)) {
						log.log(Level.FINE, "Contributor Profile update is not needed right now");
						return contributorCode;
					}

				}
			}
		}

		log.log(Level.INFO, "Going to update contributor profile for {0}={1}", new String[] { contributorCodeType,
				contributorCodeValue });

		ContributorProfile profile = takeProfileFromProvider(contributorCodeType, contributorCodeValue);
		if (profile == null) {
			return contributorCode;
		}

		contributorCode = contributorService.createOrUpdateFromProfile(profile, contributorCodeType, contributorCodeValue);

		updateContributorProfileInSearchIndex(contributorCode, profile);

		return contributorCode;
	}

	/**
	 * Take contributor profile from provider (ie. download it from remote server etc)
	 *
	 * @param contributorCodeType to take profile for
	 * @param contributorCodeValue to take profile for
	 * @return profile data or null if not found
	 */
	protected ContributorProfile takeProfileFromProvider(String contributorCodeType, String contributorCodeValue) {
		if (!isContributorCodeTypesSupported(contributorCodeType)) {
			throw new IllegalArgumentException("Unsupported contributorCodeType " + contributorCodeType);
		}

		ContributorProfile profile = contributorProfileProvider.getProfile(contributorCodeValue);
		if (profile == null) {
			log.log(Level.WARNING, "User not found in contributor profile provider for {0}={1}", new String[] {
					contributorCodeType, contributorCodeValue });
		}
		return profile;
	}

	protected void updateContributorProfileInSearchIndex(String contributorCode, ContributorProfile profile) {
		Map<String, Object> profileData = profile.getProfileData();
		putContributorCodeIntoContent(contributorCode, profileData);

		// Search profiles with same sys_contributors and update them.
		SearchResponse matchingProfiles = findByContributorCode(contributorCode);
		if (matchingProfiles != null && matchingProfiles.getHits().getTotalHits() > 0) {
			if (matchingProfiles.getHits().getTotalHits() > 1) {
				log.log(Level.WARNING, "Data inconsistency: Contributor has more than one profile in search index. "
						+ "Going to update first one and delete others. Contributor code: {0}", contributorCode);
			}
			boolean first = true;
			for (SearchHit p : matchingProfiles.getHits().getHits()) {
				if (first) {
					updateSearchIndex(p.getId(), profileData);
					first = false;
				} else {
					try {
						searchClientService.performDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, p.getId());
					} catch (SearchIndexMissingException e) {
						// this should never happen, but to be sure
					}
				}
			}
		} else {
			updateSearchIndex(profile.getId(), profileData);
		}
	}

	/**
	 * @param contributorCode
	 * @param profileData
	 */
	protected void putContributorCodeIntoContent(String contributorCode, Map<String, Object> profileData) {
		List<String> contributors = new ArrayList<>(1);
		contributors.add(contributorCode);
		profileData.put(ContentObjectFields.SYS_CONTRIBUTORS, contributors);
	}

	/**
	 * Find Contributor Profile for contributor with defined code.
	 *
	 * @param contributorCode to find for
	 * @return search response, null in case of missing index
	 */
	public SearchResponse findByContributorCode(String contributorCode) {
		try {
			return searchClientService.performFilterByOneField(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE,
					ContentObjectFields.SYS_CONTRIBUTORS, contributorCode);
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

	/**
	 * Find Contributor Profile for contributor with defined code.
	 *
	 * @param code to delete contributor for
	 * @return true if profile has been really deleted (it existed)
	 */
	public boolean deleteByContributorCode(String code) {
		SearchResponse matchingProfiles = findByContributorCode(code);
		if (matchingProfiles != null && matchingProfiles.getHits().getTotalHits() > 0) {
			for (SearchHit p : matchingProfiles.getHits().getHits()) {
				try {
					searchClientService.performDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, p.getId());
				} catch (SearchIndexMissingException e) {
					// OK
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Full synchronization of contributors and their profiles for given contributorCodeType. If contributor entry
	 * is missing then is created and corresponding contributor profile as well.
	 *
	 * @param contributorCodeType type of contributor "Type Specific Code" (eg. jboss.org username, github username etc,
	 *                            see <code>FIELD_TSC_xx</code> constants) to update profiles for.
	 * @param start pagination - start
	 * @param size paginatino - size
	 * @return number of created/updated profiles. -1 if 'contributorCodeType' is not supported or something is wrong with contributor profile provider configuration.
	 * @see #isContributorCodeTypesSupported(String)
	 */
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public int fullSynContributorsAndProfiles(String contributorCodeType, Integer start, Integer size) {
		if (!isContributorCodeTypesSupported(contributorCodeType)) {
			log.log(Level.FINE,
					"We can't sync contributors and its profiles for type specific code {0} because no profile provider is available.",
					contributorCodeType);
			return -1;
		}

		if (log.isLoggable(Level.INFO)) {
			log.log(Level.INFO, "Going to sync all contributors and its profiles for Type Specific Code: {0}, start: {1}, size: {2}",
					new Object[] {contributorCodeType, start, size});
		}

		List<ContributorProfile> profiles = contributorProfileProvider.getAllProfiles(start, size);
		if (profiles == null) {
			log.log(Level.INFO, "No profiles returned from profile provider");
			return -1;
		}
		for (ContributorProfile profile : profiles) {
			String contributorCode = ContributorService.createContributorId(profile.getFullName(), profile.getPrimaryEmail());
			String contributorCodeValue = (String) profile.getProfileData().get(ContentObjectFields.SYS_CONTENT_ID);

			updateProfileAndContributorFromProfile(profile, contributorCode, contributorCodeType, contributorCodeValue);
		}

		return profiles.size();
	}

	/**
	 * Create or Update contributor profiles for all Contributors who have filled defined Contributor's
	 * "Type Specific Code".
	 * <p>
	 * This method may run really long time!
	 *
	 * @param contributorCodeType type of contributor "Type Specific Code" (eg. jboss.org username, github username etc,
	 *          see <code>FIELD_TSC_xx</code> constants) to update profiles for.
	 * @return number of created/updated profiles. -1 if 'contributorCodeType' is not supported.
	 */
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public int createOrUpdateAllProfiles(String contributorCodeType) {
		if (isContributorCodeTypesSupported(contributorCodeType)) {
			int ret = 0;
			SearchResponse allContributors = contributorService.findByTypeSpecificCodeExistence(contributorCodeType);
			if (allContributors != null && allContributors.getHits().getTotalHits() > 0) {
				log.log(Level.INFO, "Going to update {0} contributor profiles for Type Specific Code: " + contributorCodeType,
						allContributors.getHits().getTotalHits());
				allContributors = searchClientService.executeESScrollSearchNextRequest(allContributors);
				while (allContributors.getHits().getHits().length > 0) {
					for (SearchHit p : allContributors.getHits()) {
						ret++;
						String contributorEntityId = p.getId();
						Map<String, Object> contributorEntityContent = p.getSource();
						String contributorCode = ContributorService.getContributorCode(contributorEntityContent);
						try {
							if (contributorCode == null) {
								log.log(Level.WARNING, "Data inconsistency: Contributor with id '{0}' has no 'code'.",
										contributorEntityId);
							} else {
								String contributorCodeValue = ContributorService.getContributorTypeSpecificCodeFirst(
										contributorEntityContent, contributorCodeType);
								ContributorProfile profile = takeProfileFromProvider(contributorCodeType, contributorCodeValue);
								if (profile != null) {
									updateProfileAndContributorFromProfile(profile, contributorCode, contributorCodeType, contributorCodeValue);
								} else {
									log.log(
											Level.WARNING,
											"We are unable to obtain profile data for Contributor with code '{0}' for type specific code {1}={2}.",
											new Object[] { contributorCode, contributorCodeType, contributorCodeValue });
								}
							}
						} catch (Exception e) {
							log.log(Level.WARNING, "ContributorProfile update failed for Contributor with code=" + contributorCode
									+ " due " + e.getMessage(), e);
						}
					}
					allContributors = searchClientService.executeESScrollSearchNextRequest(allContributors);
				}
			}
			return ret;
		} else {
			log.log(Level.FINE,
					"We can't update profiles for type specific code {1} because no profile provider is available.",
					contributorCodeType);
			return -1;
		}
	}

	protected void updateProfileAndContributorFromProfile(ContributorProfile profile, String contributorCode, String contributorCodeType, String contributorCodeValue) {
		// update Contributor record to add latest codes used for mappings
		contributorService.createOrUpdateFromProfile(profile, contributorCodeType, contributorCodeValue);
		// update Contributor profile
		updateContributorProfileInSearchIndex(contributorCode, profile);

	}

	/**
	 * CDI Event handler for {@link ContributorDeletedEvent} used to remove profile when contributor is deleted.
	 *
	 * @param event to process
	 */
	public void contributorDeletedEventHandler(@Observes ContributorDeletedEvent event) {
		log.log(Level.FINE, "contributorDeletedEventHandler called for event {0}", event);
		if (event != null && event.getContributorCode() != null) {
			deleteByContributorCode(event.getContributorCode());
		} else {
			log.warning("Invalid event " + event);
		}
	}

	/**
	 * CDI event handler for {@link ContributorMergedEvent} used to remove profile of deleted contributor.
	 *
	 * @param event
	 */
	public void contributorMergedEventHandler(@Observes ContributorMergedEvent event) {
		if (event != null && event.getContributorCodeFrom() != null) {
			deleteByContributorCode(event.getContributorCodeFrom());
		} else {
			log.warning("Invalid event " + event);
		}
	}

	/**
	 * CDI event handler for {@link ContributorCodeChangedEvent} used to change code in profile document.
	 *
	 * @param event
	 */
	public void contributorCodeChangedEventHandler(@Observes ContributorCodeChangedEvent event) {
		if (event != null && event.getContributorCodeFrom() != null && event.getContributorCodeTo() != null) {
			String codeTo = event.getContributorCodeTo();
			String codeFrom = event.getContributorCodeFrom();
			SearchResponse sr = findByContributorCode(codeFrom);
			if (sr != null && sr.getHits().getTotalHits() > 0) {
				if (sr.getHits().getTotalHits() > 1) {
					log.warning("Found more contributor profiles for Contributor code=" + codeFrom
							+ ". Going to update first one and delete others.");
				}

				if (deleteByContributorCode(codeTo)) {
					log.warning("Found contributor profiles for Contributor code=" + codeTo
							+ " which is target of code change. Going to delete it to replace it by profile from code=" + codeFrom);
				}

				boolean first = true;
				for (SearchHit sh : sr.getHits().getHits()) {
					if (first) {
						first = false;
						Map<String, Object> data = sh.getSource();
						putContributorCodeIntoContent(codeTo, data);
						updateSearchIndex(sh.getId(), data);
					} else {
						try {
							searchClientService.performDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, sh.getId());
						} catch (SearchIndexMissingException e) {
							// this should never happend and is not problem at all
						}
					}
				}
			}
		} else {
			log.warning("Invalid event " + event);
		}
	}

}
