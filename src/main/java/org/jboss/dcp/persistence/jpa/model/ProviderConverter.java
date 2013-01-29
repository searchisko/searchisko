/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.jpa.model;

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
	public String getValue(Provider jpaEntity) {
		return jpaEntity.getValue();
	}

	@Override
	public void setValue(Provider jpaEntity, String value) {
		jpaEntity.setValue(value);
	}

	@Override
	public Provider convertToModel(Map<String, Object> jsonMap) throws IOException {
		Provider p = new Provider();
		p.setName(jsonMap.get("name").toString());
		updateValue(p, jsonMap);
		return p;
	}

}
