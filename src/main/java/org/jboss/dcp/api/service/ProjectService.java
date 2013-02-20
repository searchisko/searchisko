/**
 * 
 */
package org.jboss.dcp.api.service;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.jboss.dcp.persistence.service.EntityService;

/**
 * Service related to project
 * 
 * @author Libor Krzyzanek
 * @author Lukas Vlcek
 * 
 */
@Named
@Stateless
public class ProjectService implements EntityService {

	/**
	 * Field in project definition with project code, used as unique ID
	 */
	public static final String CODE = "code";

	public static final String SEARCH_INDEX_NAME = "dcp_projects";

	public static final String SEARCH_INDEX_TYPE = "project";

	@Inject
	protected Logger log;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	@Named("projectServiceBackend")
	protected EntityService entityService;

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
		searchClientService.getClient().prepareIndex(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id).setSource(entity).execute()
				.actionGet();
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
		searchClientService.getClient().prepareDelete(SEARCH_INDEX_NAME, SEARCH_INDEX_TYPE, id).execute().actionGet();
	}
}
