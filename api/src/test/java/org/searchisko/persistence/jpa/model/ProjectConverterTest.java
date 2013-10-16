/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.searchisko.api.service.ProjectService;
import org.junit.Test;

/**
 * @author Libor Krzyzanek
 *
 */
public class ProjectConverterTest {

	@Test
	public void testConvertToModel() throws IOException {
		ProjectConverter converter = new ProjectConverter();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put(ProjectService.CODE, "as7");
		data.put("name", "AS 7");

		Project p = converter.convertToModel("as7", data);

		assertEquals("as7", p.getCode());
		assertEquals("{\"name\":\"AS 7\",\"" + ProjectService.CODE + "\":\"as7\"}", p.getValue());
	}

}
