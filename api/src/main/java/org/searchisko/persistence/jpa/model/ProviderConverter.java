/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

/**
 * Converter for {@link Provider}
 * 
 * @author Libor Krzyzanek
 * 
 */
public class ProviderConverter extends StringValueConverter<Provider> {

	@Override
	public String getId(Provider jpaEntity) {
		return jpaEntity.getName();
	}

	@Override
	public String getEntityIdFieldName() {
		return "name";
	}

	@Override
	public String getValue(Provider jpaEntity) {
		return jpaEntity.getValue();
	}

	@Override
	public void setValue(Provider jpaEntity, String value) {
		jpaEntity.setValue(value);
	}

	@Override
	public Provider convertToModel(String id, Map<String, Object> jsonMap) throws IOException {
		Provider p = new Provider();
		p.setName(id);
		updateValue(p, jsonMap);
		return p;
	}

}
