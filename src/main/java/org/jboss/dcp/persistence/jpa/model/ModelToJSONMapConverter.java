/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * Converter interface for converting between JPA entity and JSON value
 * 
 * @author Libor Krzyzanek
 */
public interface ModelToJSONMapConverter<T> {

	public T convertToModel(String id, Map<String, Object> jsonMap) throws IOException;

	public Map<String, Object> convertToJsonMap(T jpaEntity) throws JsonParseException, JsonMappingException,
			IOException;

	public void updateValue(T jpaEntity, Map<String, Object> jsonMapValue) throws IOException;

	public Object getId(T jpaEntity);
}
