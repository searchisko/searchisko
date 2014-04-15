/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

/**
 * Converter for {@link Config}
 * 
 * @author Libor Krzyzanek
 * 
 */
public class ConfigConverter extends StringValueConverter<Config> {

	@Override
	public String getId(Config jpaEntity) {
		return jpaEntity.getName();
	}

	@Override
	public String getEntityIdFieldName() {
		return "name";
	}

	@Override
	public String getValue(Config jpaEntity) {
		return jpaEntity.getValue();
	}

	@Override
	public void setValue(Config jpaEntity, String value) {
		jpaEntity.setValue(value);
	}

	@Override
	public Config convertToModel(String id, Map<String, Object> jsonMap) throws IOException {
		Config c = new Config();
		c.setName(id);
		updateValue(c, jsonMap);
		return c;
	}

}
