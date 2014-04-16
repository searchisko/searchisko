/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.Date;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.searchisko.api.service.SearchableEntityService;
import org.searchisko.api.tasker.Task;
import org.searchisko.persistence.service.ContentTuple;
import org.searchisko.persistence.service.ListRequest;

/**
 * Task used to reindex data from persistent store into ElasticSearch search indices for {@link SearchableEntityService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ReindexSearchableEntityTask extends Task {

	protected SearchableEntityService searchableEntityService;

	public ReindexSearchableEntityTask(SearchableEntityService searchableEntityService) {
		super();
		this.searchableEntityService = searchableEntityService;
	}

	@Override
	public void performTask() throws Exception {
		ListRequest lr = searchableEntityService.listRequestInit();
		int count = 0;
		try {
			if (lr.hasContent()) {
				long startTimestamp = System.currentTimeMillis();
				while (lr.hasContent()) {
					BulkRequestBuilder brb = searchableEntityService.prepareBulkRequest();
					for (ContentTuple<String, Map<String, Object>> contentTuple : lr.content()) {
						if (isCanceledOrInterrupted())
							return;
						String id = contentTuple.getId();
						Map<String, Object> content = contentTuple.getContent();
						searchableEntityService.updateSearchIndex(brb, id, content);
						count++;
					}
					brb.execute().actionGet();
					if (isCanceledOrInterrupted())
						return;
					lr = searchableEntityService.listRequestNext(lr);
				}
				// delete old entries from index which are not in persistence store anymore (so they was not updated during this
				// reindexing run)
				searchableEntityService.deleteOldFromSearchIndex(new Date(startTimestamp));
			}
		} finally {
			writeTaskLog(count + " records reindexed");
		}
	}
}
