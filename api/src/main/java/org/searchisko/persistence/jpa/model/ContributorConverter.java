/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

/**
 * Converter for {@link Contributor}
 * 
 * @author Libor Krzyzanek
 * 
 */
public class ContributorConverter extends StringValueConverter<Contributor> {

	@Override
	public String getId(Contributor jpaEntity) {
		return jpaEntity.getId();
	}

	@Override
	public String getEntityIdFieldName() {
		return "id";
	}

	@Override
	public String getValue(Contributor jpaEntity) {
		return jpaEntity.getValue();
	}

	@Override
	public void setValue(Contributor jpaEntity, String value) {
		jpaEntity.setValue(value);
	}

	@Override
	public Contributor convertToModel(String id, Map<String, Object> jsonMap) throws IOException {
		Contributor c = new Contributor();
		c.setId(id);
		updateValue(c, jsonMap);
		return c;
	}

}
