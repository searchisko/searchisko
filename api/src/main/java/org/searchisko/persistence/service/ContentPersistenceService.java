/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.List;
import java.util.Map;

/**
 * Interface for service used to persistently store Searchisko content.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface ContentPersistenceService {

	// TODO PERSISTENCE - add management operation which allows to get counts of records for all stored sys_content_types

	/**
	 * Get content with specified id
	 *
	 * @param id of entity
	 * @param sysContentType sys_content_type of content object
	 * @return entity data or null if not found
	 */
	public Map<String, Object> get(String id, String sysContentType);

	/**
	 * Store content with defined id. Update it if exists already.
	 *
	 * @param id of content object
	 * @param sysContentType sys_content_type of content object
	 * @param entity content to store
	 */
	public void store(String id, String sysContentType, Map<String, Object> entity);

	/**
	 * Delete content.
	 *
	 * @param id of content to delete
	 * @param sysContentType sys_content_type of content object
	 */
	public void delete(String id, String sysContentType);

	/**
	 * Init list request for whole content of given sysContentType. Call this first time, then call
	 * {@link #listRequestNext(ListRequest)} while {@link ListRequest#hasContent()} returns true.
	 *
	 * @param sysContentType to init request for
	 * @return request object
	 */
	public ListRequest listRequestInit(String sysContentType);

	/**
	 * Get subsequent iterations for list request.
	 *
	 * @param previous list request status
	 * @return current list request status
	 */
	public ListRequest listRequestNext(ListRequest previous);

	public static interface ListRequest {

		/**
		 * Return true if request has some content to process, false otherwise.
		 *
		 * @return true if there is some content to process. False if there is not any other content so next call to
		 *         {@link ContentPersistenceService#listRequestNext(ListRequest)} has no meaning.
		 */
		public boolean hasContent();

		/**
		 * Get content to be processed in this iteration.
		 *
		 * @return content to process.
		 */
		public List<Map<String, Object>> content();

	}

}
