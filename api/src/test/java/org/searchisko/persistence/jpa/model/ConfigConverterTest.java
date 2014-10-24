/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.searchisko.api.testtools.TestUtils;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link ConfigConverter}
 * 
 * @author Libor Krzyzanek
 * @author Lukas Vlcek
 */
public class ConfigConverterTest {

	@Test
	public void convertToModel() throws IOException {
		ConfigConverter converter = new ConfigConverter();
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> sysTitleConfig = new HashMap<>();
		sysTitleConfig.put("fragment_size", "-1");
		sysTitleConfig.put("number_of_fragments", "0");
		sysTitleConfig.put("fragment_offset", "0");

		data.put("sys_title", sysTitleConfig);

		Config c = converter.convertToModel("search_fulltext_highlight_fields", data);

		assertEquals("search_fulltext_highlight_fields", c.getName());
		TestUtils.assertJsonContent("{\"sys_title\":{" + "\"fragment_size\":\"-1\"," + "\"number_of_fragments\":\"0\","
				+ "\"fragment_offset\":\"0\"" + "}}", c.getValue());
	}

	@Test
	public void shouldConvertMoreNestedObjects() throws IOException {

		ConfigConverter converter = new ConfigConverter();
		Map<String, Object> terms = new HashMap<>();
		terms.put("field", "sys_contributors");
		terms.put("size", 20);
		Map<String, Object> aggregationType = new HashMap<>();
		aggregationType.put("terms", terms);
		Map<String, Object> configObject = new HashMap<>();
		configObject.put("top_contributors", aggregationType);

		Config c = converter.convertToModel("search_fulltext_aggregations_fields", configObject);

		assertEquals("search_fulltext_aggregations_fields", c.getName());
		TestUtils.assertJsonContent("{\"top_contributors\":{\"terms\":{\"field\":\"sys_contributors\",\"size\":20}}}",
				c.getValue());
	}
}
