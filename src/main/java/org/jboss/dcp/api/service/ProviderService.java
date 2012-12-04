/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorFactory;

/**
 * Service related to Content Provider
 * 
 * @author Libor Krzyzanek
 * 
 */
@Named
@ApplicationScoped
public class ProviderService {

	/** Key for provider's name which is unique identificator - can be same as provdier's ID **/
	public static final String NAME = "name";

	/** Key for passwor hash **/
	public static final String PASSWORD_HASH = "pwd_hash";

	/** Key for flag is provider is super provider **/
	public static final String SUPER_PROVIDER = "super_provider";

	/** Key for content type **/
	public static final String TYPE = "type";

	@Inject
	private Logger log;

	@Inject
	@Named("providerServiceBackend")
	private ElasticsearchEntityService entityService;

	@Inject
	private SecurityService securityService;

	@Inject
	protected SearchClientService searchClientService;

	public boolean authenticate(String provider, String password) {
		if (provider == null || password == null) {
			return false;
		}

		Map<String, Object> providerData = findProvider(provider);
		if (providerData == null) {
			return false;
		}
		Object hash = providerData.get(PASSWORD_HASH);
		if (hash == null) {
			log.log(Level.SEVERE, "Provider {0} doesn't have any password hash defined.", provider);
			return false;
		} else {
			return securityService.checkPwdHash(provider, password, hash.toString());
		}
	}

	public boolean isSuperProvider(String provider) {
		Map<String, Object> providerData = findProvider(provider);

		Object superProviderFlag = providerData.get(SUPER_PROVIDER);

		if (superProviderFlag != null && ((Boolean) superProviderFlag).booleanValue()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Find provider based on its name
	 * 
	 * @param name
	 * @return provider definition
	 */
	public Map<String, Object> findProvider(String name) {
		SearchRequestBuilder searchBuilder = entityService.createSearchRequestBuilder();
		searchBuilder.setFilter(FilterBuilders.queryFilter(QueryBuilders.textQuery("name", name)));
		searchBuilder.setQuery(QueryBuilders.matchAllQuery());

		SearchResponse response = entityService.search(searchBuilder);
		if (response.getHits().getTotalHits() == 1) {
			return response.getHits().getHits()[0].getSource();
		} else {
			return null;
		}
	}

	/**
	 * Find content type based on its id. Each content type needs to be unique
	 * 
	 * @param typeId
	 * @return content type definition
	 */
	public Map<String, Object> findContentType(String typeId) {
		SearchRequestBuilder searchBuilder = entityService.createSearchRequestBuilder();
		searchBuilder.setFilter(FilterBuilders.existsFilter("type." + typeId + ".dcp_type"));
		searchBuilder.setQuery(QueryBuilders.matchAllQuery());
		searchBuilder.addField("type." + typeId);

		SearchResponse response = entityService.search(searchBuilder);
		if (response.getHits().getTotalHits() == 1) {
			return response.getHits().getHits()[0].getFields().get("type." + typeId).getValue();
		} else {
			return null;
		}
	}

	public void runPreprocessors(List<Map<String, Object>> preprocessorsDef, Map<String, Object> content) {
		List<StructuredContentPreprocessor> preprocessors = StructuredContentPreprocessorFactory.createPreprocessors(
				preprocessorsDef, searchClientService.getClient());
		for (StructuredContentPreprocessor preprocessor : preprocessors) {
			content = preprocessor.preprocessData(content);
		}
	}

	public String generateDcpId(String type, String contentId) {
		return type + "-" + contentId;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getContentType(Map<String, Object> provider, String type) {
		Map<String, Object> types = (Map<String, Object>) provider.get(TYPE);
		if (types != null) {
			Set<String> typeIds = types.keySet();
			if (typeIds.contains(type)) {
				return (Map<String, Object>) types.get(type);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> getPreprocessors(Map<String, Object> typeDef) {
		return (List<Map<String, Object>>) typeDef.get("input_preprocessors");
	}

	@SuppressWarnings("unchecked")
	public static String getIndexName(Map<String, Object> typeDef) {
		if (typeDef.get("index") != null)
			return ((Map<String, Object>) typeDef.get("index")).get("name").toString();
		else
			return null;
	}

	@SuppressWarnings("unchecked")
	public static String getIndexType(Map<String, Object> typeDef) {
		if (typeDef.get("index") != null)
			return ((Map<String, Object>) typeDef.get("index")).get("type").toString();
		else
			return null;
	}

	public static String getDcpType(Map<String, Object> typeDef) {
		return typeDef.get("dcp_type").toString();
	}

	public EntityService getEntityService() {
		return entityService;
	}

}
