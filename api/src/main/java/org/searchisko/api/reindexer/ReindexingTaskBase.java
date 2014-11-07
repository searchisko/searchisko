/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.tasker.Task;

/**
 * Base abstract class for tasks used to reindex content in ElasticSearch search indices. Content matching some builder
 * is loaded from ES index using Scroll mechanism, some action is applied to document, and then it is stored back to the
 * ES index using bulk request.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public abstract class ReindexingTaskBase extends Task {

	protected ProviderService providerService;

	protected SearchClientService searchClientService;

	protected static final long ES_SCROLL_KEEPALIVE = 60 * 1000;

	public ReindexingTaskBase(ProviderService providerService, SearchClientService searchClientService) {
		super();
		this.providerService = providerService;
		this.searchClientService = searchClientService;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected ReindexingTaskBase() {
	}

	@Override
	public void performTask() throws Exception {

		try {
			int i = 0;
			if (validateTaskConfiguration()) {

				Client client = searchClientService.getClient();

				SearchRequestBuilder srb = prepareSearchRequest(client);
				srb.setScroll(new TimeValue(ES_SCROLL_KEEPALIVE)).setSearchType(SearchType.SCAN);

				SearchResponse scrollResp = srb.execute().actionGet();

				if (scrollResp.getHits().totalHits() > 0) {
					scrollResp = executeESScrollSearchNextRequest(client, scrollResp);
					while (scrollResp.getHits().getHits().length > 0) {
						BulkRequestBuilder brb = client.prepareBulk();
						for (SearchHit hit : scrollResp.getHits()) {
							if (isCanceledOrInterrupted()) {
								writeTaskLog("Processed " + i + " documents then cancelled.");
								return;
							}
							i++;
							performHitProcessing(client, brb, hit);
						}
						brb.execute().actionGet();
						if (isCanceledOrInterrupted()) {
							writeTaskLog("Processed " + i + " documents then cancelled.");
							return;
						}
						scrollResp = executeESScrollSearchNextRequest(client, scrollResp);
					}
					performPostReindexingProcessing(client);
				}
			}
			writeTaskLog("Processed " + i + " documents.");
		} catch (SettingsException e) {
			throw new Exception(e.getMessage());
		}
	}

	/**
	 * Validate task configuration, called before reindexing
	 * 
	 * @return true if we can continue with processing
	 * @throws Exception if configuration is not valid
	 */
	protected abstract boolean validateTaskConfiguration() throws Exception;

	/**
	 * Prepare search request to get ES documents to be reindexed.
	 * 
	 * @param client to be used
	 * @return search request builder
	 */
	protected abstract SearchRequestBuilder prepareSearchRequest(Client client);

	/**
	 * Process hit
	 * 
	 * @param client which can be used to access ES cluster
	 * @param brb which can be used to store document back into ES cluster
	 * @param hit to be processed
	 */
	protected abstract void performHitProcessing(Client client, BulkRequestBuilder brb, SearchHit hit);

	/**
	 * Perform some actions after whole reindexing is finished (indexes flush etc)
	 * 
	 * @param client to be used to access ES cluster functions
	 */
	protected abstract void performPostReindexingProcessing(Client client);

	private SearchResponse executeESScrollSearchNextRequest(Client client, SearchResponse scrollResp) {
		return client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(ES_SCROLL_KEEPALIVE)).execute()
				.actionGet();
	}
}
