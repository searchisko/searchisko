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

/**
 * Common converter for entity with value as a plain string
 *
 * @param <T> type of JPA entity
 *
 * @author Libor Krzyzanek
 *
 */
public abstract class StringValueConverter<T> extends CommonConverter<T> {

	/**
	 * Get value field content from JPA entity
	 *
	 * @param jpaEntity
	 * @return value field content
	 */
	public abstract String getValue(T jpaEntity);

	/**
	 * Set value content into JPA entity
	 *
	 * @param jpaEntity to set value into
	 * @param value to set
	 */
	public abstract void setValue(T jpaEntity, String value);

	@Override
	public Map<String, Object> convertToJsonMap(T jpaEntity) throws JsonParseException, JsonMappingException, IOException {
		return convertToJsonMap(getValue(jpaEntity));
	}

	@Override
	public void updateValue(T jpaEntity, Map<String, Object> jsonMapValue) throws IOException {
		String value = convertJsonMapToString(jsonMapValue);
		setValue(jpaEntity, value);
	}

}
