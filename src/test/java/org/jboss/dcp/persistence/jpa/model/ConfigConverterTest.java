/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.jpa.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;

/**
 * @author Libor Krzyzanek
 * 
 */
public class ConfigConverterTest {

	@Test
	public void testConvertToModel() throws IOException {
		ConfigConverter converter = new ConfigConverter();
		Map<String, Object> data = new HashMap<String, Object>();
		Map<String, Object> dcpTitleConfig = new HashMap<String, Object>();
		dcpTitleConfig.put("fragment_size", "-1");
		dcpTitleConfig.put("number_of_fragments", "0");
		dcpTitleConfig.put("fragment_offset", "0");

		data.put("dcp_title", dcpTitleConfig);

		Config c = converter.convertToModel("search_fulltext_highlight_fields", data);

		TestUtils.assertJsonContent("{\"dcp_title\":{" + "\"fragment_size\":\"-1\"," + "\"number_of_fragments\":\"0\","
				+ "\"fragment_offset\":\"0\"" + "}}", c.getValue());
	}
}
