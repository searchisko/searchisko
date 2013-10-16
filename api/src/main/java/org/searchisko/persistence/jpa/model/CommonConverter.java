/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.searchisko.api.util.SearchUtils;

/**
 * Common converter class from/to JSON
 *
 * @param <T> type of JPA entity
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
public abstract class CommonConverter<T> implements ModelToJSONMapConverter<T> {

	/**
	 * Convert JSON Map structure into String with JSON content.
	 *
	 * @param jsonMapValue to convert
	 * @return
	 * @throws IOException
	 */
	public String convertJsonMapToString(Map<String, Object> jsonMapValue) throws IOException {
		return SearchUtils.convertJsonMapToString(jsonMapValue);
	}

	/**
	 * Convert String with JSON content into JSON Map structure.
	 *
	 * @param jsonData string to convert
	 * @return JSON MAP structure
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public Map<String, Object> convertToJsonMap(String jsonData) throws JsonParseException, JsonMappingException,
			IOException {
		return SearchUtils.convertToJsonMap(jsonData);
	}
}
