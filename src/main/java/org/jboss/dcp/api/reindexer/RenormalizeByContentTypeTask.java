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
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.tasker.Task;
import org.jboss.elasticsearch.tools.content.InvalidDataException;

/**
 * Task used to renormalize content in ElasticSearch search indices for given dcp_content_type. Content is loaded from
 * ES index, all preprocessors are applied to it, and then it is stored back to the ES index.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByContentTypeTask extends Task {

	protected ProviderService providerService;

	protected SearchClientService searchClientService;

	protected String dcpContentType;

	private static final long ES_SCROLL_KEEPALIVE = 60 * 1000;

	public RenormalizeByContentTypeTask(ProviderService providerService, SearchClientService searchClientService,
			String dcpContentType) {
		super();
		this.providerService = providerService;
		this.searchClientService = searchClientService;
		this.dcpContentType = dcpContentType;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected RenormalizeByContentTypeTask() {

	}

	@Override
	public void performTask() throws Exception {
		Map<String, Object> typeDef = providerService.findContentType(dcpContentType);
		if (typeDef == null) {
			throw new Exception("Configuration not found for dcp_content_type " + dcpContentType);
		}

		try {
			String indexName = ProviderService.extractIndexName(typeDef, dcpContentType);
			String indexType = ProviderService.extractIndexType(typeDef, dcpContentType);

			Client client = searchClientService.getClient();

			SearchRequestBuilder srb = client.prepareSearch(indexName).setTypes(indexType).addField("_source")
					.setScroll(new TimeValue(ES_SCROLL_KEEPALIVE)).setSearchType(SearchType.SCAN);

			SearchResponse scrollResp = srb.execute().actionGet();

			if (scrollResp.hits().totalHits() > 0) {
				scrollResp = executeESScrollSearchNextRequest(client, scrollResp);
				while (scrollResp.hits().hits().length > 0) {
					BulkRequestBuilder brb = client.prepareBulk();
					for (SearchHit hit : scrollResp.getHits()) {
						if (isCanceledOrInterrupted())
							return;
						Map<String, Object> content = hit.getSource();
						String id = hit.getId();
						try {
							// Run preprocessors to normalize mapped fields
							providerService.runPreprocessors(dcpContentType,
									ProviderService.extractPreprocessors(typeDef, dcpContentType), content);
						} catch (InvalidDataException e) {
							writeTaskLog("Data error from preprocessors execution so document " + id + " is skipped: "
									+ e.getMessage());
							continue;
						}
						// put content back into search subsystem
						brb.add(client.prepareIndex(indexName, indexType, id).setSource(content));
					}
					brb.execute().actionGet();
					if (isCanceledOrInterrupted())
						return;
					scrollResp = executeESScrollSearchNextRequest(client, scrollResp);
				}
				client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
			}
		} catch (SettingsException e) {
			throw new Exception(e.getMessage());
		}
	}

	private SearchResponse executeESScrollSearchNextRequest(Client client, SearchResponse scrollResp) {
		return client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(ES_SCROLL_KEEPALIVE)).execute()
				.actionGet();
	}
}
