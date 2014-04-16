/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.service;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.StreamingOutput;

/**
 * Base interface for entity service used to persistently store Searchisko entities.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public interface EntityService {

	/**
	 * Get All entities with pagination support.
	 * 
	 * @param from from index. Can be null
	 * @param size size to return. If null then default length is returned
	 * @param fieldsToRemove array of fields that should be removed from entity data
	 * @return output with entities
	 */
	public StreamingOutput getAll(Integer from, Integer size, String[] fieldsToRemove);

	/**
	 * Get All entities.
	 * 
	 * @return output with entities
	 */
	public List<Map<String, Object>> getAll();

	/**
	 * Get entity with specified id
	 * 
	 * @param id of entity
	 * @return entity data or null if not found
	 */
	public Map<String, Object> get(String id);

	/**
	 * Create an entity and let generate id
	 * 
	 * @param entity data
	 * @return generated id for entity
	 */
	public String create(Map<String, Object> entity);

	/**
	 * Create an entity with defined id. Update it if exists already.
	 * 
	 * @param id of entity
	 * @param entity content
	 */
	public void create(String id, Map<String, Object> entity);

	/**
	 * Update content of entity, create it if doesn't exists.
	 * 
	 * @param id of entity
	 * @param entity content
	 */
	public void update(String id, Map<String, Object> entity);

	/**
	 * Delete entity
	 * 
	 * @param id of entity to delete
	 */
	public void delete(String id);

	/**
	 * Init list request for whole content of given Entity. Call this first time, then call
	 * {@link #listRequestNext(ListRequest)} while {@link ListRequest#hasContent()} returns true.
	 * 
	 * @return list request object
	 */
	public ListRequest listRequestInit();

	/**
	 * Get subsequent iterations for list request.
	 * 
	 * @param previous list request status
	 * @return current list request status
	 */
	public ListRequest listRequestNext(ListRequest previous);

}
