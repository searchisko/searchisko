/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.Map;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.elasticsearch.tools.content.InvalidDataException;

/**
 * Task used to renormalize content in ElasticSearch search indices for given dcp_content_type. Content is loaded from
 * ES index, all preprocessors are applied to it, and then it is stored back to the ES index.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByContentTypeTask extends ReindexingTaskBase {

	protected String dcpContentType;

	private String indexName;
	private String indexType;
	private Map<String, Object> typeDef;

	public RenormalizeByContentTypeTask(ProviderService providerService, SearchClientService searchClientService,
			String dcpContentType) {
		super(providerService, searchClientService);
		this.dcpContentType = dcpContentType;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeByContentTypeTask() {
	}

	@Override
	protected void validateTaskConfiguration() throws Exception {
		typeDef = providerService.findContentType(dcpContentType);
		if (typeDef == null) {
			throw new Exception("Configuration not found for dcp_content_type " + dcpContentType);
		}
		indexName = ProviderService.extractIndexName(typeDef, dcpContentType);
		indexType = ProviderService.extractIndexType(typeDef, dcpContentType);
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
			providerService.runPreprocessors(dcpContentType, ProviderService.extractPreprocessors(typeDef, dcpContentType),
					content);
		} catch (InvalidDataException e) {
			writeTaskLog("Data error from preprocessors execution so document " + id + " is skipped: " + e.getMessage());
			return;
		}
		// put content back into search subsystem
		brb.add(client.prepareIndex(indexName, indexType, id).setSource(content));
	}

	@Override
	protected void performPostReindexingProcessing(Client client) {
		client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
	}

}
