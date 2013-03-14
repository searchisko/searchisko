/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.Date;
import java.util.Map;

import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.tasker.Task;
import org.jboss.dcp.persistence.service.ContentPersistenceService;
import org.jboss.dcp.persistence.service.ContentPersistenceService.ListRequest;
import org.jboss.elasticsearch.tools.content.InvalidDataException;

/**
 * Task used to reindex data from persistent store into ElasticSearch search indices.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ReindexFromPersistenceTask implements Task {

	protected ContentPersistenceService contentPersistenceService;

	protected ProviderService providerService;

	protected SearchClientService searchClientService;

	protected String dcpContentType;

	/**
	 * @param contentPersistenceService
	 * @param providerService
	 * @param searchClientService
	 * @param dcpContentType
	 */
	public ReindexFromPersistenceTask(ContentPersistenceService contentPersistenceService,
			ProviderService providerService, SearchClientService searchClientService, String dcpContentType) {
		super();
		this.contentPersistenceService = contentPersistenceService;
		this.providerService = providerService;
		this.searchClientService = searchClientService;
		this.dcpContentType = dcpContentType;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected ReindexFromPersistenceTask() {

	}

	@Override
	public void run() {
		Map<String, Object> typeDef = providerService.findContentType(dcpContentType);
		if (typeDef == null) {
			// TODO REINDEX mark task as unsuccessful and finish it
			return;
		}

		try {
			String indexName = ProviderService.extractIndexName(typeDef, dcpContentType);
			String indexType = ProviderService.extractIndexType(typeDef, dcpContentType);

			ListRequest lr = contentPersistenceService.listRequestInit(dcpContentType);
			if (lr.hasContent()) {
				long startTimestamp = System.currentTimeMillis();
				Client client = searchClientService.getClient();
				while (lr.hasContent()) {
					BulkRequestBuilder brb = client.prepareBulk();
					for (Map<String, Object> content : lr.content()) {
						// TODO REINDEX support for task cancellation
						try {
							// Run preprocessors to normalize mapped fields
							providerService.runPreprocessors(dcpContentType,
									ProviderService.extractPreprocessors(typeDef, dcpContentType), content);
						} catch (InvalidDataException e) {
							// TODO REINDEX write error to the task log but continue processing
						}
						// TODO EXTERNAL_TAGS - add external tags for this document into dcp_tags field

						// Push to search subsystem
						brb.add(client.prepareIndex(indexName, indexType, (String) content.get(DcpContentObjectFields.DCP_ID))
								.setSource(content));
					}
					brb.execute().actionGet();
					lr = contentPersistenceService.listRequestNext(lr);
				}
				// delete old entries from index which are not in persistence store anymore (so they was not updated during this
				// reindexing run)
				client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
				FilterBuilder filterTime = FilterBuilders.rangeFilter("_timestamp").lt(new Date(startTimestamp));
				client.prepareDeleteByQuery(indexName).setTypes(indexType)
						.setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), filterTime)).execute().actionGet();
			}
		} catch (SettingsException e) {
			// TODO REINDEX mark task as unsuccessful
		} catch (Exception e) {
			// TODO REINDEX mark task as failed
		}
	}
}
