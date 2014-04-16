/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Date;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.searchisko.persistence.service.EntityService;

/**
 * Entity service which has copy of data stored in search index also.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface SearchableEntityService extends EntityService {

	/**
	 * Prepare {@link BulkRequestBuilder} to be used in {@link #updateSearchIndex(BulkRequestBuilder, String, Map)}.
	 * 
	 * @return bulk request builder
	 */
	BulkRequestBuilder prepareBulkRequest();

	/**
	 * Updates search index for entity identified by id.
	 * 
	 * @param brb to be used for update
	 * @param id of entity
	 * @param entity data to store into search index
	 * @see #prepareBulkRequest()
	 */
	void updateSearchIndex(BulkRequestBuilder brb, String id, Map<String, Object> entity);

	/**
	 * Delete all records not updated after given timestamp from search index.
	 * 
	 * @param timestamp to delete records not updated after
	 */
	void deleteOldFromSearchIndex(Date timestamp);

}
