/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.Map;

import org.searchisko.persistence.service.EntityService;

/**
 * Entity service which has copy of data stored in search index also.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface SearchableEntityService extends EntityService {

	/**
	 * Updates search index for entity identified by id.
	 * 
	 * @param id of entity
	 * @param entity data to store into search index
	 */
	void updateSearchIndex(String id, Map<String, Object> entity);

}
