/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.Map;

/**
 * Base interface for entity service
 * 
 * @author Libor Krzyzanek
 * 
 */
public interface EntityService {

	/**
	 * Get All entities
	 * 
	 * @param from
	 *            from index. Can be null
	 * @param size
	 *            size to return. If null then default length is returned
	 * @return
	 */
	public Object getAll(Integer from, Integer size);

	/**
	 * Get entity with specified id
	 * 
	 * @param id
	 * @return
	 */
	public Map<String, Object> get(String id);

	/**
	 * Create an entity and let generate id
	 * 
	 * @param entity
	 * @return
	 */
	public String create(Map<String, Object> entity);

	/**
	 * Create an entity with defined id
	 * 
	 * @param id
	 * @param entity
	 */
	public void create(String id, Map<String, Object> entity);

	/**
	 * Update content of entity
	 * 
	 * @param id
	 * @param entity
	 */
	public void update(String id, Map<String, Object> entity);

	/**
	 * Delete entity
	 * 
	 * @param id
	 */
	public void delete(String id);

}
