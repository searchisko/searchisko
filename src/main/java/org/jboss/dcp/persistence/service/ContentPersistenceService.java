/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.service;

import java.util.Map;

/**
 * Interface for service used to persistently store DCP content.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface ContentPersistenceService {

	// TODO PERSISTENCE - add operation to list content objects with some filters - dcp_content_type,
	// dcp_updated from and to

	// TODO PERSISTENCE - add management operation which allows to get counts of records for all stored dcp_content_types

	/**
	 * Get content with specified id
	 * 
	 * @param id of entity
	 * @param dcpContentType dcp_content_type of content object
	 * @return entity data or null if not found
	 */
	public Map<String, Object> get(String id, String dcpContentType);

	/**
	 * Store DCP content with defined id. Update it if exists already.
	 * 
	 * @param id of content object
	 * @param dcpContentType dcp_content_type of content object
	 * @param updateDate
	 * @param entity content to store
	 */
	public void store(String id, String dcpContentType, Map<String, Object> entity);

	/**
	 * Delete DCP content.
	 * 
	 * @param id of content to delete
	 * @param dcpContentType dcp_content_type of content object
	 */
	public void delete(String id, String dcpContentType);

}
