/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

import org.searchisko.persistence.service.ContentTuple;

/**
 * Converter interface for converting between JPA entity and JSON value.
 * 
 * @param <T> type of JPA entity
 * 
 * @author Libor Krzyzanek
 */
public interface ModelToJSONMapConverter<T> {

	/**
	 * Create JPA entity for given ID and JSON Map value
	 * 
	 * @param id of entity
	 * @param jsonMap value to store into entity
	 * @return JPA entity
	 * @throws IOException
	 */
	public T convertToModel(String id, Map<String, Object> jsonMap) throws IOException;

	/**
	 * Convert value from jpaEntity into JSON Map structure.
	 * 
	 * @param jpaEntity to get value from
	 * @return JSON Map structure from entity value
	 * @throws IOException
	 */
	public Map<String, Object> convertToJsonMap(T jpaEntity) throws IOException;

	/**
	 * Convert value from jpaEntity into ContentTuple which contains identifier and data in for m of JSON Map structure.
	 * 
	 * @param jpaEntity to get value from
	 * @return ContentTuple with entity id and data
	 * @throws IOException
	 */
	public ContentTuple<String, Map<String, Object>> convertToContentTuple(T jpaEntity) throws IOException;

	/**
	 * Update value in JPA entity from jsonMapValue
	 * 
	 * @param jpaEntity to update data into
	 * @param jsonMapValue to update data from
	 * @throws IOException
	 */
	public void updateValue(T jpaEntity, Map<String, Object> jsonMapValue) throws IOException;

	/**
	 * Get ID from JPA entity.
	 * 
	 * @param jpaEntity
	 * @return ID
	 */
	public String getId(T jpaEntity);

	/**
	 * Get name if ID field for JPA queries.
	 * 
	 * @return name of ID field
	 */
	public String getEntityIdFieldName();

}
