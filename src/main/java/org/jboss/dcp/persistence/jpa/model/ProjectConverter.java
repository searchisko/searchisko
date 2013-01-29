/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.jpa.model;

import java.io.IOException;
import java.util.Map;

/**
 * Converter for {@link Project}
 * 
 * @author Libor Krzyzanek
 * 
 */
public class ProjectConverter extends StringValueConverter<Project> {

	@Override
	public String getValue(Project jpaEntity) {
		return jpaEntity.getValue();
	}

	@Override
	public void setValue(Project jpaEntity, String value) {
		jpaEntity.setValue(value);
	}

	@Override
	public Project convertToModel(Map<String, Object> jsonMap) throws IOException {
		Project p = new Project();
		p.setCode(jsonMap.get("code").toString());
		updateValue(p, jsonMap);
		return p;
	}
}
