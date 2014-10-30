/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

/**
 * Converter for {@link org.searchisko.persistence.jpa.model.Query}
 *
 * @author Lukas Vlcek
 *
 */
public class QueryConverter extends StringValueConverter<Query> {

	@Override
	public String getId(Query jpaEntity) {
		return jpaEntity.getName();
	}

	@Override
	public String getEntityIdFieldName() {
		return "name";
	}

	@Override
	public String getValue(Query jpaEntity) {
		return jpaEntity.getValue();
	}

	@Override
	public void setValue(Query jpaEntity, String value) {
		jpaEntity.setValue(value);
	}

	@Override
	public Query convertToModel(String id, Map<String, Object> jsonMap) throws IOException {
		Query q = new Query();
		q.setName(id);
		updateValue(q, jsonMap);
		return q;
	}

}
