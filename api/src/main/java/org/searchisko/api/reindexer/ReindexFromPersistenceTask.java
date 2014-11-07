/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.Date;
import java.util.Map;

import javax.enterprise.event.Event;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.SettingsException;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.rest.exception.PreprocessorInvalidDataException;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.tasker.Task;
import org.searchisko.persistence.service.ContentPersistenceService;
import org.searchisko.persistence.service.ContentTuple;
import org.searchisko.persistence.service.ListRequest;

/**
 * Task used to reindex data from persistent store into ElasticSearch search indices.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ReindexFromPersistenceTask extends Task {

	protected ContentPersistenceService contentPersistenceService;

	protected ProviderService providerService;

	protected SearchClientService searchClientService;

	protected String sysContentType;

	protected Event<ContentBeforeIndexedEvent> eventBeforeIndexed;

	public ReindexFromPersistenceTask(ContentPersistenceService contentPersistenceService,
			ProviderService providerService, SearchClientService searchClientService,
			Event<ContentBeforeIndexedEvent> eventBeforeIndexed, String sysContentType) {
		super();
		this.contentPersistenceService = contentPersistenceService;
		this.providerService = providerService;
		this.searchClientService = searchClientService;
		this.sysContentType = sysContentType;
		this.eventBeforeIndexed = eventBeforeIndexed;
	}

	/**
	 * Constructor for unit tests.
	 */
	protected ReindexFromPersistenceTask() {

	}

	@Override
	public void performTask() throws Exception {
		ProviderContentTypeInfo typeInfo = providerService.findContentType(sysContentType);
		if (typeInfo == null) {
			throw new Exception("Configuration not found for sys_content_type " + sysContentType);
		}

		int count = 0;
		try {
			String indexName = ProviderService.extractIndexName(typeInfo, sysContentType);
			String indexType = ProviderService.extractIndexType(typeInfo, sysContentType);
			ListRequest lr = contentPersistenceService.listRequestInit(sysContentType);
			if (lr.hasContent()) {
				long startTimestamp = System.currentTimeMillis();
				Client client = searchClientService.getClient();
				while (lr.hasContent()) {
					BulkRequestBuilder brb = client.prepareBulk();
					for (ContentTuple<String, Map<String, Object>> contentTuple : lr.content()) {
						if (isCanceledOrInterrupted())
							return;
						String id = contentTuple.getId();
						Map<String, Object> content = contentTuple.getContent();
						try {
							// Run preprocessors to normalize mapped fields
							providerService.runPreprocessors(sysContentType,
									ProviderService.extractPreprocessors(typeInfo, sysContentType), content);
						} catch (PreprocessorInvalidDataException e) {
							writeTaskLog("Data error from preprocessors execution so document " + id + " is skipped: "
									+ e.getMessage());
							continue;
						}

						eventBeforeIndexed.fire(new ContentBeforeIndexedEvent(id, content));

						// Push to search subsystem
						brb.add(client.prepareIndex(indexName, indexType, id).setSource(content));
						count++;
					}
					brb.execute().actionGet();
					if (isCanceledOrInterrupted())
						return;
					lr = contentPersistenceService.listRequestNext(lr);
				}
				// delete old entries from index which are not in persistence store anymore (so they was not updated during this
				// reindexing run)
				searchClientService.performDeleteOldRecords(indexName, indexType, new Date(startTimestamp));
			}
		} catch (SettingsException e) {
			throw new Exception(e.getMessage());
		} finally {
			writeTaskLog(count + " records reindexed");
		}
	}
}
