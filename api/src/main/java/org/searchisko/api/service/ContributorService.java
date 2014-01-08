/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.reindexer.ReindexingTaskFactory;
import org.searchisko.api.reindexer.ReindexingTaskTypes;
import org.searchisko.api.tasker.TaskConfigurationException;
import org.searchisko.api.tasker.UnsupportedTaskException;
import org.searchisko.api.util.Resources;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.contribprofile.model.ContributorProfile;
import org.searchisko.persistence.service.EntityService;

/**
 * Service containing Contributor related operations.
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

	@Override
	public String create(Map<String, Object> entity) {
		String id = entityService.create(entity);

		updateSearchIndex(id, entity);

		return id;
	}

	@Override
	public void create(String id, Map<String, Object> entity) {
		entityService.create(id, entity);
		updateSearchIndex(id, entity);
	}

	@Override
	public void update(String id, Map<String, Object> entity) {
		entityService.update(id, entity);
		updateSearchIndex(id, entity);
	}

	@Override
	public void delete(String id) {
		entityService.delete(id);
		searchClientService.performDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id);
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
		} catch (IndexMissingException e) {
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
		} catch (IndexMissingException e) {
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
		} catch (IndexMissingException e) {
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
	 * Create or update Contributor record from {@link ContributorProfile} informations.
	 * 
	 * @param profile to create/update Contributor from
	 * @param typeSpecificCodeField profile has been loaded for. Can be null not to use this code to search for
	 *          Contributor.
	 * @param typeSpecificCode profile has been loaded for.
	 * @return contributor code (unique contributor id within Searchisko used in other documents) for Contributor record
	 */
	public String createOrUpdateFromProfile(ContributorProfile profile, String typeSpecificCodeField,
			String typeSpecificCode) {

		List<String> toRenormalizeContributorIds = new ArrayList<>();

		String contributorCode = createContributorId(profile.getFullName(), profile.getPrimaryEmail());

		SearchHit contributorById = null;
		SearchResponse srById = findByCode(contributorCode);
		if (srById.getHits().getTotalHits() > 0) {
			if (srById.getHits().getTotalHits() > 1) {
				log.warning("Contributor configuration problem! We found more Contributor definitions for code=" + contributorCode
						+ ". For now we use first one, but problem should be resolved by administrator!");
			}
			contributorById = srById.getHits().getHits()[0];
		}

		SearchHit contributorByTsc = null;
		contributorByTsc = findOneByTypeSpecificCode(typeSpecificCodeField, typeSpecificCode, contributorCode);

		String contributorEntityId = null;
		Map<String, Object> contributorEntityContent = null;
		if (contributorById != null && contributorByTsc != null) {
			contributorEntityId = contributorById.getId();
			contributorEntityContent = contributorById.getSource();
			String ciFromTsc = getContributorCode(contributorByTsc.getSource());
			if (!contributorCode.equals(ciFromTsc)) {
				log.info("Contributor duplicity detected. We are going to merge contributor '" + ciFromTsc
						+ "' into contributor '" + contributorCode + "'");
				toRenormalizeContributorIds.add(ciFromTsc);
				// TODO CONTRIBUTOR_PROFILE merge data from contributorByTsc into contributorEntityContent
				delete(contributorByTsc.getId());
			}
		} else if (contributorById == null && contributorByTsc != null) {
			contributorCode = getContributorCode(contributorByTsc.getSource());
			contributorEntityId = contributorByTsc.getId();
			contributorEntityContent = contributorByTsc.getSource();
		} else if (contributorById != null && contributorByTsc == null) {
			contributorEntityId = contributorById.getId();
			contributorEntityContent = contributorById.getSource();
		} else {
			// TODO CONTRIBUTOR_PROFILE try to find existing Contributor by any of email addresses available in profile
			contributorEntityContent = new HashMap<String, Object>();
			contributorEntityContent.put(ContributorService.FIELD_CODE, contributorCode);
		}

		// TODO CONTRIBUTOR_PROFILE upgrade contributorEntityContent with new values from profile
		// TODO CONTRIBUTOR_PROFILE check email uniqueness (remove them from other Contributor records if any and reindex)

		if (contributorEntityId != null)
			create(contributorEntityId, contributorEntityContent);
		else
			create(contributorEntityContent);

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

		return contributorCode;

	}

	private SearchHit findOneByTypeSpecificCode(String typeSpecificCodeField, String typeSpecificCode, String expectedCode) {
		SearchHit contributorByTsc = null;
		if (typeSpecificCodeField != null) {
			SearchResponse srByTsc = findByTypeSpecificCode(typeSpecificCodeField, typeSpecificCode);
			if (srByTsc.getHits().getTotalHits() > 0) {
				if (srByTsc.getHits().getTotalHits() > 1) {
					log.warning("Contributor configuration problem! We found more Contributor definitions for "
							+ ContributorService.FIELD_TYPE_SPECIFIC_CODE
							+ "/"
							+ typeSpecificCodeField
							+ "="
							+ typeSpecificCode
							+ ". For now we try to find correct one over 'code' or use first one, but problem should be resolved by administrator!");
					for (SearchHit sh : srByTsc.getHits()) {
						if (expectedCode.equals(getContributorCode(sh.getSource()))) {
							contributorByTsc = sh;
							break;
						}
					}
				}
				if (contributorByTsc == null)
					contributorByTsc = srByTsc.getHits().getHits()[0];
				if (SearchUtils.isBlank(getContributorCode(contributorByTsc.getSource()))) {
					String msg = "Contributor configuration problem! 'code' field is empty for Contributor with id="
							+ contributorByTsc.getId() + " so we can't use it. Skipping this record for now.";
					log.log(Level.WARNING, msg);
					contributorByTsc = null;
				}
			}
		}
		return contributorByTsc;
	}

	/**
	 * Get contributor code (unique contributor id within Searchisko used in other documents) from Contributor data
	 * structure.
	 * 
	 * @param contribStructure to get code from
	 * @return contributor code
	 */
	protected static String getContributorCode(Map<String, Object> contribStructure) {
		return (String) contribStructure.get(ContributorService.FIELD_CODE);
	}

}
