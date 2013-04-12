/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.elasticsearch.tools.content.InvalidDataException;

/**
 * Base for tasks used to renormalize document in ES search indices. All documents for specified filters (implemented in
 * {@link #addFilters(SearchRequestBuilder)}) are loaded from all ES indices with dcp content, all preprocessors are
 * applied to content, and then it is stored back to the ES index.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class RenormalizeTaskBase extends ReindexingTaskBase {

	protected Set<String> indexNames = new HashSet<String>();
	protected Set<String> indexTypes = new HashSet<String>();

	/**
	 * @param providerService
	 * @param searchClientService
	 */
	public RenormalizeTaskBase(ProviderService providerService, SearchClientService searchClientService) {
		super(providerService, searchClientService);
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeTaskBase() {
	}

	@Override
	protected boolean validateTaskConfiguration() throws Exception {
		loadIndexNamesAndTypesForWholeContent();
		return !indexNames.isEmpty();
	}

	@Override
	protected SearchRequestBuilder prepareSearchRequest(Client client) {
		SearchRequestBuilder srb = client.prepareSearch(getIndexNamesAsArray()).setTypes(getIndexTypesAsArray())
				.addField("_source");
		addFilters(srb);
		return srb;
	}

	/**
	 * Add filters to select content to be reindexed. Called from {@link #prepareSearchRequest(Client)}.
	 * 
	 * @param srb to add filters into
	 */
	protected abstract void addFilters(SearchRequestBuilder srb);

	@Override
	protected void performHitProcessing(Client client, BulkRequestBuilder brb, SearchHit hit) {
		Map<String, Object> content = hit.getSource();
		String id = hit.getId();
		String dcpContentType = (String) content.get(DcpContentObjectFields.DCP_CONTENT_TYPE);
		Map<String, Object> typeDef = providerService.findContentType(dcpContentType);
		if (typeDef == null) {
			writeTaskLog("No type definition found for document id=" + id + " so is skipped");
		} else {
			try {
				// Run preprocessors to normalize mapped fields
				providerService.runPreprocessors(dcpContentType, ProviderService.extractPreprocessors(typeDef, dcpContentType),
						content);
			} catch (InvalidDataException e) {
				writeTaskLog("ERROR: Data error from preprocessors execution so document " + id + " is skipped: "
						+ e.getMessage());
				return;
			}
			// put content back into search subsystem
			brb.add(client.prepareIndex(ProviderService.extractIndexName(typeDef, dcpContentType),
					ProviderService.extractIndexType(typeDef, dcpContentType), id).setSource(content));
		}
	}

	@Override
	protected void performPostReindexingProcessing(Client client) {
		if (indexNames.size() > 0)
			client.admin().indices().flush(new FlushRequest(getIndexNamesAsArray()).refresh(true)).actionGet();
	}

	/**
	 * Fill {@link #indexNames} and {@link #indexTypes} fields with all indices and types for all configured DCP content
	 * types.
	 * 
	 * @see ProviderService#getAll()
	 */
	protected void loadIndexNamesAndTypesForWholeContent() {
		List<Map<String, Object>> providers = providerService.getAll();

		for (Map<String, Object> providerDef : providers) {
			Map<String, Map<String, Object>> allTypes = ProviderService.extractAllContentTypes(providerDef);
			if (allTypes != null) {
				for (String dcpContentType : allTypes.keySet()) {
					Map<String, Object> typeDef = allTypes.get(dcpContentType);
					indexNames.add(ProviderService.extractIndexName(typeDef, dcpContentType));
					indexTypes.add(ProviderService.extractIndexType(typeDef, dcpContentType));
				}
			}
		}
	}

	/**
	 * @return {@link #indexNames} as string array to be passed into ES API methods
	 */
	protected String[] getIndexNamesAsArray() {
		return indexNames.toArray(new String[indexNames.size()]);
	}

	/**
	 * @return {@link #indexTypes} as string array to be passed into ES API methods
	 */
	protected String[] getIndexTypesAsArray() {
		return indexTypes.toArray(new String[indexTypes.size()]);
	}

}
