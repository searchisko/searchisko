/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.rest.exception.PreprocessorInvalidDataException;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.service.SearchClientService;

/**
 * Task used to renormalize content in ElasticSearch search indices for given sys_content_type. Content is loaded from
 * ES index, all preprocessors are applied to it, and then it is stored back to the ES index.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByContentTypeTask extends ReindexingTaskBase {

	protected String sysContentType;

	protected String indexName;
	protected String indexType;
	protected ProviderContentTypeInfo typeDef;

	public RenormalizeByContentTypeTask(ProviderService providerService, SearchClientService searchClientService,
			String sysContentType) {
		super(providerService, searchClientService);
		this.sysContentType = sysContentType;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeByContentTypeTask() {
	}

	@Override
	protected boolean validateTaskConfiguration() throws Exception {
		typeDef = providerService.findContentType(sysContentType);
		if (typeDef == null) {
			throw new Exception("Configuration not found for sys_content_type " + sysContentType);
		}
		indexName = ProviderService.extractIndexName(typeDef, sysContentType);
		indexType = ProviderService.extractIndexType(typeDef, sysContentType);
		return true;
	}

	@Override
	protected SearchRequestBuilder prepareSearchRequest(Client client) {
		return client.prepareSearch(indexName).setTypes(indexType).addField("_source");
	}

	@Override
	protected void performHitProcessing(Client client, BulkRequestBuilder brb, SearchHit hit) {
		Map<String, Object> content = hit.getSource();
		String id = hit.getId();
		try {
			// Run preprocessors to normalize mapped fields
			providerService.runPreprocessors(sysContentType, ProviderService.extractPreprocessors(typeDef, sysContentType),
					content);
		} catch (PreprocessorInvalidDataException e) {
			writeTaskLog("ERROR: Data error from preprocessors execution so document " + id + " is skipped: "
					+ e.getMessage());
			return;
		}
		// put content back into search subsystem
		brb.add(client.prepareIndex(indexName, indexType, id).setSource(content));
	}

	@Override
	protected void performPostReindexingProcessing(Client client) {
		client.admin().indices().prepareFlush(indexName).execute().actionGet();
		client.admin().indices().prepareRefresh(indexName).execute().actionGet();
	}

}
