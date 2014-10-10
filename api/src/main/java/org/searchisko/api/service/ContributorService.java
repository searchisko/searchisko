/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.collections.ListUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.events.ContributorCodeChangedEvent;
import org.searchisko.api.events.ContributorCreatedEvent;
import org.searchisko.api.events.ContributorDeletedEvent;
import org.searchisko.api.events.ContributorMergedEvent;
import org.searchisko.api.events.ContributorUpdatedEvent;
import org.searchisko.api.events.RolesUpdatedEvent;
import org.searchisko.api.reindexer.ReindexingTaskFactory;
import org.searchisko.api.reindexer.ReindexingTaskTypes;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.AuthenticatedUserType;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.UnsupportedTaskException;
import org.searchisko.api.util.Resources;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.persistence.service.EntityService;
import org.searchisko.persistence.service.ListRequest;

/**
 * Service containing Contributor related operations.
 * <p>
 * This service fires CDI events:
 * <ul>
 * <li> {@link ContributorCreatedEvent}
 * <li> {@link ContributorUpdatedEvent}
 * <li> {@link ContributorDeletedEvent}
 * <li> {@link ContributorMergedEvent}
 * <li> {@link ContributorCodeChangedEvent}
 * </ul>
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
@Named
@Stateless
@LocalBean
public class ContributorService implements SearchableEntityService {

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
	 * Contributor document field containing full name of contributor.
	 */
	public static final String FIELD_NAME = "name";

	/**
	 * Contributor document field containing array of roles.
	 *
	 * @see #getRolesByTypeSpecificCode(String, String)
	 */
	public static final String FIELD_ROLES = "roles";

	/**
	 * Contributor hire date
	 */
	public static final String FIELD_HIRE_DATE = "hire_date";

	/**
	 * Contributor leave date
	 */
	public static final String FIELD_LEAVE_DATE = "leave_date";

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
	protected Event<ContributorCreatedEvent> eventCreate;

	@Inject
	protected Event<ContributorUpdatedEvent> eventUpdate;

	@Inject
	protected Event<RolesUpdatedEvent> eventRolesUpdate;

	@Inject
	protected Event<ContributorDeletedEvent> eventDelete;

	@Inject
	protected Event<ContributorMergedEvent> eventContributorMerged;

	@Inject
	protected Event<ContributorCodeChangedEvent> eventContributorCodeChanged;

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

	protected void updateSearchIndex(String id, Map<String, Object> entity) {
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
		boolean rolesChanged = false;
		if (exists && fireEvent) {
			rolesChanged = isRolesChanged(id, entity);
		}

		entityService.create(id, entity);
		updateSearchIndex(id, entity);

		if (fireEvent) {
			if (exists) {
				ContributorUpdatedEvent event = new ContributorUpdatedEvent(id, newCode, entity);
				log.log(Level.FINE, "Going to fire event {0}", event);
				eventUpdate.fire(event);

				if (rolesChanged) {
					RolesUpdatedEvent rolesUpdatedEvent = new RolesUpdatedEvent(
							AuthenticatedUserType.CONTRIBUTOR,
							entity);
					log.log(Level.FINE, "Going to fire roles changed event {0}", rolesUpdatedEvent);
					eventRolesUpdate.fire(rolesUpdatedEvent);
				}
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
		updateImplRaw(id, entity);

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

	private void updateImplRaw(String id, Map<String, Object> entity) {
		entityService.update(id, entity);
		updateSearchIndex(id, entity);
	}

	private void validateCodeUniqueness(String newCode, String id) throws BadFieldException {
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
	 * @throws BadFieldException if code is not equal
	 */
	private boolean validateCodeNotChanged(String id, String newCode) throws BadFieldException {
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

	private String validateCodeRequired(Map<String, Object> entity) throws RequiredFieldException {
		String newCode = SearchUtils.trimToNull(getContributorCode(entity));
		if (newCode == null) {
			throw new RequiredFieldException(FIELD_CODE);
		}
		return newCode;
	}

	protected boolean isRolesChanged(String id, Map<String, Object> entity) {
		Map<String, Object> oldEntity = get(id);
		if (oldEntity != null) {
			Set<String> oldRoles = extractRoles(oldEntity);
			Set<String> newRoles = extractRoles(entity);
			return !ListUtils.isEqualList(oldRoles, newRoles);
		}
		return false;
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
	 * Find contributor by name.
	 *
	 * @param name to search contributor for.
	 * @param exactMatch if <code>true</code> then exact match over name is performed, if <code>false</code> then fulltext
	 *          search is performed.
	 * @return search result - should contain zero, one or more contributors.
	 */
	public SearchResponse findByName(String name, boolean exactMatch) {
		try {
			String fn = FIELD_NAME;
			if (!exactMatch)
				fn += ".fulltext";
			return searchClientService.performFilterByOneField(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, fn, name);
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
	 * <p>
	 * Be careful, this method returns SCROLL response, so you have to use
	 * {@link SearchClientService#executeESScrollSearchNextRequest(SearchResponse)} to get real data and go over them as
	 * is common ES scroll mechanism.
	 *
	 * @param codeName name of 'type specific code', eg. <code>jbossorg_username</code>, <code>github_username</code>
	 * @return search result - should contain zero or more contributors - scrollable!!.
	 */
	public SearchResponse findByTypeSpecificCodeExistence(String codeName) {
		try {
			return searchClientService.performQueryByOneFieldAnyValue(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE,
					FIELD_TYPE_SPECIFIC_CODE + "." + codeName, "_id");
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

	/**
	 * Get roles for contributor identified by username in type specific field named by codeName attribute.
	 * <br/>
	 * Roles are defined in {@link #FIELD_ROLES}
	 * @param codeName name of 'type specific code', eg. <code>jbossorg_username</code>, <code>github_username</code>
	 * @param username third party identifier
	 * @return list of roles or null if no roles defined
	 * @see org.searchisko.api.service.ContributorProfileService#FIELD_TSC_JBOSSORG_USERNAME
	 */
	public Set<String> getRolesByTypeSpecificCode(String codeName, String username) {
		SearchResponse sr = findByTypeSpecificCode(codeName, username);
		if (sr != null) {
			if (sr.getHits().getTotalHits() > 1) {
				log.log(Level.SEVERE,
						"Configuration problem! Contributor has more than one exact matching contributors! Type specific code: {0}, value: {1}",
						new Object[]{codeName, username});
				return null;
			}
			if (sr.getHits().getTotalHits() == 1) {
				Map<String, Object> fields = sr.getHits().getHits()[0].getSource();
				return extractRoles(fields);
			} else {
				log.log(Level.FINE, "No contributor found");
			}
		}
		return null;
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
	 * Extract roles from contributor entry
	 *
	 * @param fields
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Set<String> extractRoles(Map<String, Object> fields) {
		if (fields.containsKey(FIELD_ROLES)) {
			List<String> list = (List<String>) fields.get(FIELD_ROLES);
			return new HashSet<String>(list);
		}
		return null;
	}

	/**
	 * Create or update Contributor record from {@link ContributorProfile} information loaded from provider using type
	 * specific code.
	 * <p>
	 * A {@link ContributorCreatedEvent} or {@link ContributorUpdatedEvent} is fired for affected Contributor.
	 * {@link ContributorMergedEvent} may be fired also if this method detects that merge is necessary. Series of
	 * {@link ContributorUpdatedEvent} events for other Contributors may be fired also if this method patches uniqueness
	 * of <code>email</code>s and other <code>type_specific_code</code>s.
	 *
	 * @param profile to create/update Contributor from
	 * @param typeSpecificCodeField profile has been loaded for. Can be null not to use this code to search for
	 *          Contributor.
	 * @param typeSpecificCodeValue profile has been loaded for.
	 * @return contributor code (unique contributor id within Searchisko used in other documents) for Contributor record
	 */
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public String createOrUpdateFromProfile(ContributorProfile profile, String typeSpecificCodeField,
			String typeSpecificCodeValue) {

		Set<String> toRenormalizeContributorCodes = new HashSet<>();

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
				toRenormalizeContributorCodes.add(contributorCodeFromTsc);
				mergeContributorData(contributorEntityContent, contributorByTsc.getSource());
				deleteImpl(contributorByTsc.getId());
				ContributorMergedEvent event = new ContributorMergedEvent(contributorCodeFromTsc,
						getContributorCode(contributorEntityContent));
				log.log(Level.FINE, "Going to fire event {0}", event);
				eventContributorMerged.fire(event);
				log.fine("ContributorMergedEvent fire finished");
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
				contributorEntityContent = new HashMap<>();
				contributorEntityContent.put(ContributorService.FIELD_CODE, contributorCode);
			}
		}
		// Always update name, hire, leave date from profile
		contributorEntityContent.put(FIELD_NAME, profile.getFullName());

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		sdf.setLenient(false);
		if (profile.getHireDate() != null) {
			String value = sdf.format(profile.getHireDate());
			contributorEntityContent.put(FIELD_HIRE_DATE, value);
		}
		if (profile.getLeaveDate() != null) {
			String value = sdf.format(profile.getLeaveDate());
			contributorEntityContent.put(FIELD_LEAVE_DATE, value);
		}
		Map<String, Object> newDataFromProfile = new HashMap<>();
		newDataFromProfile.put(FIELD_EMAIL, profile.getEmails());
		newDataFromProfile.put(FIELD_TYPE_SPECIFIC_CODE, profile.getTypeSpecificCodes());
		mergeContributorData(contributorEntityContent, newDataFromProfile);

		patchEmailUniqueness(toRenormalizeContributorCodes, contributorEntityId, contributorEntityContent);
		patchTypeSpecificCodeUniqueness(toRenormalizeContributorCodes, contributorEntityId, contributorEntityContent);

		if (contributorEntityId != null)
			create(contributorEntityId, contributorEntityContent, true);
		else
			create(contributorEntityContent, true);

		searchClientService.performIndexFlushAndRefreshBlocking(SEARCH_INDEX_NAME);

		if (!toRenormalizeContributorCodes.isEmpty()) {
			String description = "contributor '" + contributorCode + "' update from profile"
					+ (typeSpecificCodeField != null ? " loaded for " + typeSpecificCodeField + "=" + typeSpecificCodeValue : "");
			createContentRenormalizationTask(toRenormalizeContributorCodes, description);
		}
		return contributorCode;
	}

	protected void createContentRenormalizationTask(Set<String> toRenormalizeContributorCodes, String description) {
		if (!toRenormalizeContributorCodes.isEmpty()) {
			if (log.isLoggable(Level.FINE))
				log.fine("We are going to run content renormalization task for contributor codes: "
						+ toRenormalizeContributorCodes);
			Map<String, Object> taskConfig = new HashMap<String, Object>();
			taskConfig.put(ReindexingTaskFactory.CFG_CONTRIBUTOR_CODE, toRenormalizeContributorCodes);
			taskConfig.put("description", description);
			try {
				taskService.getTaskManager().createTask(ReindexingTaskTypes.RENORMALIZE_BY_CONTRIBUTOR_CODE.getTaskType(),
						taskConfig);
			} catch (UnsupportedTaskException | TaskConfigurationException e) {
				log.severe("Problem to start Contributor renormalization task: " + e.getMessage());
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void patchEmailUniqueness(Set<String> toRenormalizeContributorIds, String contributorEntityId,
			Map<String, Object> contributorEntityContent) {
		try {
			List<String> emails = (List<String>) contributorEntityContent.get(FIELD_EMAIL);
			if (emails != null && !emails.isEmpty()) {
				for (String email : emails) {
					if (SearchUtils.isBlank(email))
						continue;
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
		if (SearchUtils.isBlank(codeValue))
			return;
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

		// temporarily remove code and name from mergeFromContributor to preserve only one from mergeToContributor
		Object oCode = mergeFromContributor.remove(FIELD_CODE);
		Object oName = null;
		if (mergeToContributor.get(FIELD_NAME) != null) {
			oName = mergeFromContributor.remove(FIELD_NAME);
		}

		SearchUtils.mergeJsonMaps(mergeFromContributor, mergeToContributor);

		// return back
		if (oCode != null)
			mergeFromContributor.put(FIELD_CODE, oCode);
		if (mergeToContributor.get(FIELD_NAME) != null && oName != null) {
			mergeFromContributor.put(FIELD_NAME, oName);
		}
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

	/**
	 * Change code of contributor to new value. Content reindexation task is started to maintain data consistency.
	 *
	 * @param id of contributor to change code for
	 * @param code new code
	 * @return contributor's data after change
	 * @throws ObjectNotFoundException
	 */
	public Map<String, Object> changeContributorCode(String id, String code) throws ObjectNotFoundException {

		code = SearchUtils.trimToNull(code);
		if (code == null) {
			throw new RequiredFieldException(FIELD_CODE);
		}

		Map<String, Object> entity = get(id);
		if (entity == null)
			throw new ObjectNotFoundException(id);

		String codeFrom = getContributorCode(entity);

		// no real change in code so stop processing early
		if (code.equals(codeFrom)) {
			return entity;
		}

		validateCodeUniqueness(code, id);
		entity.put(FIELD_CODE, code);

		updateImplRaw(id, entity);

		Set<String> toRenormalizeContributorCodes = new HashSet<>();
		toRenormalizeContributorCodes.add(codeFrom);
		createContentRenormalizationTask(toRenormalizeContributorCodes, "Change of contributor's code from '" + codeFrom
				+ "' to '" + code + "' for Contributor.id=" + id);

		eventContributorCodeChanged.fire(new ContributorCodeChangedEvent(codeFrom, code));

		return entity;
	}

	/**
	 * Merge two contributors into one. Content reindexation task is started to maintain data consistency.
	 *
	 * @param idFrom identifier of Contributor who is source of merge (so is deleted at the end)
	 * @param idTo identifier of Contributor who is target of merge (so is kept at the end)
	 * @return final Contributor definition which is result of merge
	 * @throws ObjectNotFoundException
	 */
	public Map<String, Object> mergeContributors(String idFrom, String idTo) throws ObjectNotFoundException {
		Map<String, Object> entityFrom = get(idFrom);
		if (entityFrom == null)
			throw new ObjectNotFoundException(idFrom);

		Map<String, Object> entityTo = get(idTo);
		if (entityTo == null)
			throw new ObjectNotFoundException(idTo);

		String codeFrom = getContributorCode(entityFrom);
		String codeTo = getContributorCode(entityTo);
		if (SearchUtils.trimToNull(codeTo) == null) {
			throw new BadFieldException("idTo", "'code' is not defined for this Contributor");
		}

		mergeContributorData(entityTo, entityFrom);

		updateImplRaw(idTo, entityTo);
		deleteImpl(idFrom);

		if (codeFrom != null) {
			Set<String> toRenormalizeContributorCodes = new HashSet<>();
			toRenormalizeContributorCodes.add(codeFrom);
			createContentRenormalizationTask(toRenormalizeContributorCodes, "Merge of contributor's code from '" + codeFrom
					+ "' to '" + codeTo + "' with final Contributor.id=" + idTo);
			eventContributorMerged.fire(new ContributorMergedEvent(codeFrom, codeTo));
		} else {
			// only sanity check on data integrity, hope it will never happen
			log.warning("No code found for merged Contributor.id=" + idFrom + " so reindex and event skipped");
		}

		return entityTo;
	}

	@Override
	public ListRequest listRequestInit() {
		return entityService.listRequestInit();
	}

	@Override
	public ListRequest listRequestNext(ListRequest previous) {
		return entityService.listRequestNext(previous);
	}

	@Override
	public BulkRequestBuilder prepareBulkRequest() {
		return searchClientService.getClient().prepareBulk();
	}

	@Override
	public void updateSearchIndex(BulkRequestBuilder brb, String id, Map<String, Object> entity) {
		brb.add(searchClientService.getClient().prepareIndex(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id).setSource(entity));
	}

	@Override
	public void deleteOldFromSearchIndex(Date timestamp) {
		searchClientService.performDeleteOldRecords(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, timestamp);
	}

}
