/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.events.ContributorCreatedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.api.events.ContributorUpdatedEvent;
import org.searchisko.api.reindexer.ReindexingTaskFactory;
import org.searchisko.api.reindexer.ReindexingTaskTypes;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.UnsupportedTaskException;
import org.searchisko.api.util.Resources;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.RatingPersistenceService;

/**
 * Service containing Contributor related operations.
 * <p>
 * This service fires CDI events:
 * <ul>
 * <li> {@link ContributorCreatedEvent}
 * <li> {@link ContributorUpdatedEvent}
 * <li> {@link ContributorDeletedEvent}
 * </ul>
 * TODO CONTRIBUTOR implements code change/merge operations
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
@Named
@Stateless
@LocalBean
public class ContributorService implements EntityService {

	@Inject
	protected Logger log;

	public static final String SEARCH_INDEX_NAME = "sys_contributors";

	public static final String SEARCH_INDEX_TYPE = "contributor";

	/**
	 * Contributor document field containing "code" - primary unique id of Contributor for searchisko.
	 */
	public static final String FIELD_CODE = "code";

	/**
	 * Contributor document field containing array of email addresses used by this contributor. Note than one email
	 * address should be defined for one contributor only!
	 */
	public static final String FIELD_EMAIL = "email";

	/**
	 * Contributor document field containing Map structure with other unique identifiers used to map pushed data to the
	 * contributor. Key in the Map structure marks type of identifier (eg. jbossorg_username, github_username), value in
	 * structure is identifier or array of identifiers itself used during mapping.
	 * 
	 * @see {@link #findByTypeSpecificCode(String, String)} for description.
	 */
	public static final String FIELD_TYPE_SPECIFIC_CODE = "type_specific_code";

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	@Named("contributorServiceBackend")
	protected EntityService entityService;

	@Inject
	protected TaskService taskService;

	@Inject
	protected ContributorProfileService contributorProfileService;

	@Inject
	protected RatingPersistenceService ratingPersistenceService;

	@Inject
	protected Event<ContributorCreatedEvent> eventCreate;
	@Inject
	protected Event<ContributorUpdatedEvent> eventUpdate;

	@Inject
	protected Event<ContributorDeletedEvent> eventDelete;

	@PostConstruct
	public void init() {
		try {
			Client client = searchClientService.getClient();
			if (!client.admin().indices().prepareExists(SEARCH_INDEX_NAME).execute().actionGet().isExists()) {
				log.info("Contributor search index called '" + SEARCH_INDEX_NAME
						+ "' doesn't exists. Creating it together with mapping for type '" + SEARCH_INDEX_TYPE + "'");
				client.admin().indices().prepareCreate(SEARCH_INDEX_NAME).execute().actionGet();
				client.admin().indices().preparePutMapping(SEARCH_INDEX_NAME).setType(SEARCH_INDEX_TYPE)
						.setSource(Resources.readStringFromClasspathFile("/mappings/contributor.json")).execute().actionGet();
			} else {
				log.info("Contributor search index called '" + SEARCH_INDEX_NAME + "' exists already.");
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public StreamingOutput getAll(Integer from, Integer size, String[] fieldsToRemove) {
		return entityService.getAll(from, size, fieldsToRemove);
	}

	@Override
	public List<Map<String, Object>> getAll() {
		return entityService.getAll();
	}

	@Override
	public Map<String, Object> get(String id) {
		return entityService.get(id);
	}

	/**
	 * Updates search index by current entity identified by id
	 * 
	 * @param id
	 * @param entity
	 */
	private void updateSearchIndex(String id, Map<String, Object> entity) {
		searchClientService.performPut(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id, entity);
		searchClientService.performIndexFlushAndRefresh(SEARCH_INDEX_NAME);
	}

	/**
	 * Create contributor and generate id for it.
	 * <p>
	 * A {@link ContributorCreatedEvent} is fired.
	 */
	@Override
	public String create(Map<String, Object> entity) {
		return create(entity, true);
	}

	/**
	 * Same as {@link #create(Map)} but allows to control if {@link ContributorCreatedEvent} is fired or not.
	 * 
	 * @param entity data about contributor
	 * @param fireEvent true to fire ContributorCreatedEvent
	 * @return id of created contributor
	 */
	public String create(Map<String, Object> entity, boolean fireEvent) {

		String newCode = validateCodeRequired(entity);
		validateCodeUniqueness(newCode, null);

		String id = entityService.create(entity);

		updateSearchIndex(id, entity);

		if (fireEvent) {
			ContributorCreatedEvent event = new ContributorCreatedEvent(id, newCode, entity);
			log.log(Level.FINE, "Going to fire event {0}", event);
			eventCreate.fire(event);
		}

		return id;
	}

	/**
	 * Create or update Contributor for given id.
	 * <p>
	 * A {@link ContributorCreatedEvent} or {@link ContributorUpdatedEvent} is fired.
	 */
	@Override
	public void create(String id, Map<String, Object> entity) {
		create(id, entity, true);
	}

	/**
	 * Same as {@link #create(String, Map)} but allows to control if Event is fired or not.
	 * 
	 * @param id of contributor
	 * @param entity
	 * @param fireEvent true to fire events
	 */
	public void create(String id, Map<String, Object> entity, boolean fireEvent) {

		String newCode = validateCodeRequired(entity);
		validateCodeUniqueness(newCode, id);
		boolean exists = validateCodeNotChanged(id, newCode);

		entityService.create(id, entity);
		updateSearchIndex(id, entity);

		if (fireEvent) {
			if (exists) {
				ContributorUpdatedEvent event = new ContributorUpdatedEvent(id, newCode, entity);
				log.log(Level.FINE, "Going to fire event {0}", event);
				eventUpdate.fire(event);
			} else {
				ContributorCreatedEvent event = new ContributorCreatedEvent(id, newCode, entity);
				log.log(Level.FINE, "Going to fire event {0}", event);
				eventCreate.fire(event);
			}
		}

	}

	/**
	 * Update or create Contributor for given id.
	 * <p>
	 * A {@link ContributorCreatedEvent} or {@link ContributorUpdatedEvent} is fired.
	 */
	@Override
	public void update(String id, Map<String, Object> entity) {
		update(id, entity, true);
	}

	/**
	 * Same as {@link #update(String, Map)} but allows to control if Event is fired or not.
	 * 
	 * @param id of contributor
	 * @param entity
	 * @param fireEvent true to fire events
	 */
	public void update(String id, Map<String, Object> entity, boolean fireEvent) {
		String newCode = validateCodeRequired(entity);
		boolean exists = validateCodeNotChanged(id, newCode);
		entityService.update(id, entity);
		updateSearchIndex(id, entity);

		if (fireEvent) {
			if (exists) {
				ContributorUpdatedEvent event = new ContributorUpdatedEvent(id, newCode, entity);
				log.log(Level.FINE, "Going to fire event {0}", event);
				eventUpdate.fire(event);
			} else {
				ContributorCreatedEvent event = new ContributorCreatedEvent(id, newCode, entity);
				log.log(Level.FINE, "Going to fire event {0}", event);
				eventCreate.fire(event);
			}
		}
	}

	private void validateCodeUniqueness(String newCode, String id) {
		if (newCode == null)
			return;
		SearchHit sh = findOneByCode(newCode);
		if (sh != null && (id == null || !id.equals(sh.getId()))) {
			throw new BadFieldException(FIELD_CODE, "Provided 'code' value '" + newCode
					+ "' is duplicit with contributor.id=" + sh.getId() + " value: " + getContributorCode(sh.getSource()));
		}
	}

	/**
	 * @param id of contributor
	 * @param newCode to validate against old code
	 * @return true if old entity exists for given id
	 */
	private boolean validateCodeNotChanged(String id, String newCode) {
		Map<String, Object> oldEntity = get(id);
		if (oldEntity != null) {
			String oldCode = SearchUtils.trimToNull(getContributorCode(oldEntity));
			if (oldCode != null && !newCode.equals(oldCode)) {
				throw new BadFieldException(FIELD_CODE,
						"contributor code can't be changed by plain 'update' operation, use 'merge' operation instead.");
			}
			return true;
		}
		return false;
	}

	private String validateCodeRequired(Map<String, Object> entity) {
		String newCode = SearchUtils.trimToNull(getContributorCode(entity));
		if (newCode == null) {
			throw new RequiredFieldException(FIELD_CODE);
		}
		return newCode;
	}

	/**
	 * Delete contributor.
	 * <p>
	 * A {@link ContributorDeletedEvent} is fired if contributor existed.
	 */
	@Override
	public void delete(String id) {
		String code = deleteImpl(id);
		if (code != null) {
			ContributorDeletedEvent event = new ContributorDeletedEvent(id, SearchUtils.trimToNull(code));
			log.log(Level.FINE, "Going to fire event {0}", event);
			eventDelete.fire(event);
			log.fine("ContributorDeletedEvent fire finished");
		}
	}

	/**
	 * Real implementation of delete, do not fire event.
	 * 
	 * @param id of contributor to delete.
	 * @return contributor code. Null if contributor is not found.
	 */
	protected String deleteImpl(String id) {
		Map<String, Object> entity = entityService.get(id);
		entityService.delete(id);
		try {
			searchClientService.performDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id);
			searchClientService.performIndexFlushAndRefresh(SEARCH_INDEX_NAME);
		} catch (SearchIndexMissingException e) {
			// OK
		}
		if (entity == null)
			return null;
		return getContributorCode(entity);
	}

	/**
	 * Find contributor by <code>code</code> (unique id used in content).
	 * 
	 * @param code to search contributor for.
	 * 
	 * @return search result - should contain zero or one contributor only! Multiple contributors for one code is
	 *         configuration problem!
	 */
	public SearchResponse findByCode(String code) {
		try {
			return searchClientService.performFilterByOneField(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, FIELD_CODE, code);
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

	/**
	 * Find contributor by email.
	 * 
	 * @param email address to search contributor for.
	 * 
	 * @return search result - should contain zero or one contributor only! Multiple contributors for one email address is
	 *         configuration problem!
	 */
	public SearchResponse findByEmail(String email) {
		try {
			return searchClientService.performFilterByOneField(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, FIELD_EMAIL, email);
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

	/**
	 * Find contributor by 'type specific code'. These codes are used to map from third party unique identifiers to
	 * searchisko unique contributor id.
	 * 
	 * @param codeName name of 'type specific code', eg. <code>jbossorg_username</code>, <code>github_username</code>
	 * @param codeValue value of code to search for
	 * @return search result - should contain zero or one contributor only! Multiple contributors for one code is
	 *         configuration problem!
	 */
	public SearchResponse findByTypeSpecificCode(String codeName, String codeValue) {
		try {
			return searchClientService.performFilterByOneField(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, FIELD_TYPE_SPECIFIC_CODE
					+ "." + codeName, codeValue);
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

	/**
	 * Find contributor which has any value available for given 'type specific code'. These codes are used to map from
	 * third party unique identifiers to searchisko unique contributor id.
	 * 
	 * @param codeName name of 'type specific code', eg. <code>jbossorg_username</code>, <code>github_username</code>
	 * @return search result - should contain zero or more contributors.
	 */
	public SearchResponse findByTypeSpecificCodeExistence(String codeName) {
		try {
			return searchClientService.performQueryByOneFieldAnyValue(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE,
					FIELD_TYPE_SPECIFIC_CODE + "." + codeName);
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

	/**
	 * Create contributor ID from user related informations.
	 * 
	 * @param fullName of user
	 * @param email primary email of user
	 * @return contributor id
	 */
	public static String createContributorId(String fullName, String email) {
		fullName = SearchUtils.trimToNull(fullName);
		if (fullName == null)
			throw new IllegalArgumentException("fullName can't be empty");
		email = SearchUtils.trimToNull(email);
		if (email == null)
			throw new IllegalArgumentException("email can't be empty");
		return fullName + " <" + email.toLowerCase() + ">";
	}

	/**
	 * Extract contributor name from contributor id string. So extracts 'John Doe' from '
	 * <code>John Doe <john@doe.org></code>'.
	 * 
	 * @param contributorID id to extract name from
	 * @return contributor name
	 */
	public static String extractContributorName(String contributorID) {
		if (contributorID == null)
			return null;
		int i = contributorID.lastIndexOf("<");
		int i2 = contributorID.lastIndexOf(">");
		if (i > -1 && i2 > -1 && i < i2) {
			return SearchUtils.trimToNull(contributorID.substring(0, i));
		}
		return SearchUtils.trimToNull(contributorID);
	}

	/**
	 * Create or update Contributor record from {@link ContributorProfile} informations.
	 * 
	 * @param profile to create/update Contributor from
	 * @param typeSpecificCodeField profile has been loaded for. Can be null not to use this code to search for
	 *          Contributor.
	 * @param typeSpecificCodeValue profile has been loaded for.
	 * @return contributor code (unique contributor id within Searchisko used in other documents) for Contributor record
	 */
	public String createOrUpdateFromProfile(ContributorProfile profile, String typeSpecificCodeField,
			String typeSpecificCodeValue) {

		Set<String> toRenormalizeContributorIds = new HashSet<>();

		String contributorCode = createContributorId(profile.getFullName(), profile.getPrimaryEmail());

		searchClientService.performIndexFlushAndRefreshBlocking(SEARCH_INDEX_NAME);
		SearchHit contributorById = findOneByCode(contributorCode);
		SearchHit contributorByTsc = findOneByTypeSpecificCode(typeSpecificCodeField, typeSpecificCodeValue,
				contributorCode);

		String contributorEntityId = null;
		Map<String, Object> contributorEntityContent = null;
		if (contributorById != null && contributorByTsc != null) {
			contributorEntityId = contributorById.getId();
			contributorEntityContent = contributorById.getSource();
			String contributorCodeFromTsc = getContributorCode(contributorByTsc.getSource());
			if (!contributorCode.equals(contributorCodeFromTsc)) {
				log.info("Contributor duplicity detected. We are going to merge contributor '" + contributorCodeFromTsc
						+ "' into contributor '" + contributorCode + "'");
				toRenormalizeContributorIds.add(contributorCodeFromTsc);
				mergeContributorData(contributorEntityContent, contributorByTsc.getSource());
				deleteImpl(contributorByTsc.getId());
				contributorProfileService.deleteByContributorCode(contributorCodeFromTsc);
				ratingPersistenceService.mergeRatingsForContributors(contributorCodeFromTsc,
						getContributorCode(contributorEntityContent));
			}
		} else if (contributorById == null && contributorByTsc != null) {
			contributorCode = getContributorCode(contributorByTsc.getSource());
			contributorEntityId = contributorByTsc.getId();
			contributorEntityContent = contributorByTsc.getSource();
		} else if (contributorById != null && contributorByTsc == null) {
			contributorEntityId = contributorById.getId();
			contributorEntityContent = contributorById.getSource();
		} else {
			SearchHit contributorByEmail = findOneByEmail(profile.getPrimaryEmail(), profile.getEmails());
			if (contributorByEmail != null) {
				contributorCode = getContributorCode(contributorByEmail.getSource());
				contributorEntityId = contributorByEmail.getId();
				contributorEntityContent = contributorByEmail.getSource();
			} else {
				contributorEntityContent = new HashMap<String, Object>();
				contributorEntityContent.put(ContributorService.FIELD_CODE, contributorCode);
			}
		}

		Map<String, Object> newDataFromProfile = new HashMap<>();
		newDataFromProfile.put(FIELD_EMAIL, profile.getEmails());
		newDataFromProfile.put(FIELD_TYPE_SPECIFIC_CODE, profile.getTypeSpecificCodes());
		mergeContributorData(contributorEntityContent, newDataFromProfile);

		patchEmailUniqueness(toRenormalizeContributorIds, contributorEntityId, contributorEntityContent);
		patchTypeSpecificCodeUniqueness(toRenormalizeContributorIds, contributorEntityId, contributorEntityContent);

		if (contributorEntityId != null)
			create(contributorEntityId, contributorEntityContent, false);
		else
			create(contributorEntityContent, false);

		searchClientService.performIndexFlushAndRefreshBlocking(SEARCH_INDEX_NAME);

		if (!toRenormalizeContributorIds.isEmpty()) {
			if (log.isLoggable(Level.FINE))
				log.fine("We are going to renormalize content for contributor codes: " + toRenormalizeContributorIds);
			Map<String, Object> taskConfig = new HashMap<String, Object>();
			taskConfig.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, toRenormalizeContributorIds);
			taskConfig
					.put("description", "contributor '"
							+ contributorCode
							+ "' update from profile"
							+ (typeSpecificCodeField != null ? " loaded for " + typeSpecificCodeField + "=" + typeSpecificCodeValue
									: ""));
			try {
				taskService.getTaskManager().createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(),
						taskConfig);
			} catch (UnsupportedTaskException | TaskConfigurationException e) {
				log.severe("Problem to start contributor renormalization task: " + e.getMessage());
			}
		}
		return contributorCode;
	}

	@SuppressWarnings("unchecked")
	protected void patchEmailUniqueness(Set<String> toRenormalizeContributorIds, String contributorEntityId,
			Map<String, Object> contributorEntityContent) {
		try {
			List<String> emails = (List<String>) contributorEntityContent.get(FIELD_EMAIL);
			if (emails != null && !emails.isEmpty()) {
				for (String email : emails) {
					searchClientService.performIndexFlushAndRefreshBlocking(SEARCH_INDEX_NAME);
					SearchResponse sr = findByEmail(email);
					if (sr != null) {
						for (SearchHit sh : sr.getHits().getHits()) {
							if (!contributorEntityId.equals(sh.getId())) {
								Map<String, Object> shData = sh.getSource();
								List<String> shEmails = (List<String>) shData.get(FIELD_EMAIL);
								if (shEmails != null) {
									shEmails.remove(email);
									create(sh.getId(), shData);
									toRenormalizeContributorIds.add(getContributorCode(shData));
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.warning("Problem during Contributor's email uniqueness patching: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	protected void patchTypeSpecificCodeUniqueness(Set<String> toRenormalizeContributorIds, String contributorEntityId,
			Map<String, Object> contributorEntityContent) {
		try {
			Map<String, Object> tsc = (Map<String, Object>) contributorEntityContent.get(FIELD_TYPE_SPECIFIC_CODE);
			if (tsc != null) {
				for (String typeSpecificCodeField : tsc.keySet()) {
					Object o = tsc.get(typeSpecificCodeField);
					if (o != null) {
						if (o instanceof List) {
							List<Object> cvl = (List<Object>) o;
							if (cvl != null && !cvl.isEmpty()) {
								for (Object cv : cvl) {
									patchTypeSpecificCodeUniqueness(toRenormalizeContributorIds, contributorEntityId,
											typeSpecificCodeField, cv);
								}
							}
						} else if ((o instanceof String) || (o instanceof Number)) {
							patchTypeSpecificCodeUniqueness(toRenormalizeContributorIds, contributorEntityId, typeSpecificCodeField,
									o);
						} else {
							log.warning("Unsupported Contributor's type_specific_code." + typeSpecificCodeField
									+ " value type for contributor entity id: " + contributorEntityId);
						}
					}
				}
			}
		} catch (Exception e) {
			log.warning("Problem during Contributor's type_specific_code uniqueness patching: " + e.getMessage());
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void patchTypeSpecificCodeUniqueness(Set<String> toRenormalizeContributorIds, String contributorEntityId,
			String codeName, Object codeValue) {
		searchClientService.performIndexFlushAndRefreshBlocking(SEARCH_INDEX_NAME);
		SearchResponse sr = findByTypeSpecificCode(codeName, codeValue.toString());
		if (sr != null) {
			for (SearchHit sh : sr.getHits().getHits()) {
				if (!contributorEntityId.equals(sh.getId())) {
					Map<String, Object> shData = sh.getSource();
					Map<String, Object> tsc = (Map<String, Object>) shData.get(FIELD_TYPE_SPECIFIC_CODE);
					Object o = tsc.get(codeName);
					if (o != null) {
						if (o instanceof List) {
							((List) o).remove(codeValue);
						} else if (!(o instanceof Map)) {
							tsc.remove(codeName);
						} else {
							log.warning("Unsupported Contributor's type_specific_code." + codeName
									+ " value type for contributor entity id: " + sh.getId());
						}
						create(sh.getId(), shData);
						toRenormalizeContributorIds.add(getContributorCode(shData));
					}
				}
			}
		}
	}

	protected void mergeContributorData(Map<String, Object> mergeToContributor, Map<String, Object> mergeFromContributor) {
		if (mergeToContributor == null)
			throw new IllegalArgumentException("mergeToContributor can't be null");
		if (mergeFromContributor == null || mergeFromContributor.isEmpty())
			return;

		// temporarily remove code from mergeFromContributor to preserve only one from mergeToContributor
		Object o = mergeFromContributor.remove(FIELD_CODE);
		SearchUtils.mergeJsonMaps(mergeFromContributor, mergeToContributor);
		if (o != null)
			mergeFromContributor.put(FIELD_CODE, o);
	}

	protected SearchHit findOneByCode(String contributorCode) {
		SearchHit sh = null;
		SearchResponse sr = findByCode(contributorCode);
		if (sr != null) {
			log.fine("Number of Contributor records found by code=" + contributorCode + " is " + sr.getHits().getTotalHits());
			if (sr.getHits().getTotalHits() > 0) {
				if (sr.getHits().getTotalHits() > 1) {
					log.warning("Contributor configuration problem! We found more Contributor definitions for code="
							+ contributorCode + ". For now we use first one, but problem should be resolved by administrator!");
				}
				sh = sr.getHits().getHits()[0];
			}
		}
		return sh;
	}

	protected SearchHit findOneByTypeSpecificCode(String typeSpecificCodeField, String typeSpecificCodeValue,
			String expectedCode) {
		SearchHit contributorByTsc = null;
		if (typeSpecificCodeField != null) {
			SearchResponse sr = findByTypeSpecificCode(typeSpecificCodeField, typeSpecificCodeValue);
			if (sr != null) {
				log.fine("Number of Contributor records found by " + ContributorService.FIELD_TYPE_SPECIFIC_CODE + "."
						+ typeSpecificCodeField + "=" + typeSpecificCodeValue + " is " + sr.getHits().getTotalHits());
				if (sr.getHits().getTotalHits() > 0) {
					if (sr.getHits().getTotalHits() > 1) {
						log.warning("Contributor configuration problem! We found more Contributor definitions for "
								+ ContributorService.FIELD_TYPE_SPECIFIC_CODE
								+ "."
								+ typeSpecificCodeField
								+ "="
								+ typeSpecificCodeValue
								+ ". For now we try to find correct one over 'code' or use first one, but problem should be resolved by administrator!");
						if (expectedCode != null) {
							for (SearchHit sh : sr.getHits()) {
								if (expectedCode.equals(getContributorCode(sh.getSource()))) {
									contributorByTsc = sh;
									break;
								}
							}
						}
					}
					if (contributorByTsc == null)
						contributorByTsc = sr.getHits().getHits()[0];
					if (SearchUtils.isBlank(getContributorCode(contributorByTsc.getSource()))) {
						String msg = "Contributor configuration problem! 'code' field is empty for Contributor with id="
								+ contributorByTsc.getId() + " so we can't use it. Skipping this record for now.";
						log.log(Level.WARNING, msg);
						contributorByTsc = null;
					}
				}
			}
		}
		return contributorByTsc;
	}

	protected SearchHit findOneByEmail(String primaryEmail, List<String> emails) {
		try {
			if (primaryEmail != null) {
				searchClientService.performIndexFlushAndRefreshBlocking(SEARCH_INDEX_NAME);
				SearchResponse sr = findByEmail(primaryEmail);
				if (sr != null) {
					log.fine("Number of Contributor records found by primaryEmail=" + primaryEmail + " is "
							+ sr.getHits().getTotalHits());
					if (sr.getHits().getTotalHits() > 0) {
						return sr.getHits().getHits()[0];
					}
				}
			}
			if (emails != null) {
				for (String email : emails) {
					if (primaryEmail == null || !primaryEmail.equals(email)) {
						SearchResponse sr = findByEmail(email);
						if (sr != null) {
							log.fine("Number of Contributor records found by email=" + email + " is " + sr.getHits().getTotalHits());
							if (sr.getHits().getTotalHits() > 0) {
								return sr.getHits().getHits()[0];
							}
						}
					}
				}
			}
			return null;
		} catch (IndexMissingException e) {
			return null;
		}
	}

	/**
	 * Get contributor code (unique contributor id within Searchisko used in other documents) from Contributor data
	 * structure.
	 * 
	 * @param contribStructure to get code from
	 * @return contributor code
	 */
	public static String getContributorCode(Map<String, Object> contribStructure) {
		return (String) contribStructure.get(ContributorService.FIELD_CODE);
	}

	/**
	 * Get first value of 'Contributor type specific code' from Contributor data structure.
	 * 
	 * @param contribStructure to get code from
	 * @param contributorCodeType type of code to get
	 * @return first code of given type or null
	 */
	@SuppressWarnings("unchecked")
	public static String getContributorTypeSpecificCodeFirst(Map<String, Object> contribStructure,
			String contributorCodeType) {
		try {
			Map<String, Object> tsc = (Map<String, Object>) contribStructure.get(ContributorService.FIELD_TYPE_SPECIFIC_CODE);
			if (tsc != null) {
				Object o = tsc.get(contributorCodeType);
				if (o instanceof Map) {
					// bad structure
				} else if (o instanceof List) {
					List<Object> l = (List<Object>) o;
					if (l.size() > 0) {
						return l.get(0).toString();
					}
				} else if (o != null) {
					return o.toString();
				}
			}
		} catch (ClassCastException e) {
			// bad structure of contributor record, nothing to return
		}
		return null;
	}

}
