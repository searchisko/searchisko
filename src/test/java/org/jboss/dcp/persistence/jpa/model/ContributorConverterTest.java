/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.persistence.jpa.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Test;

/**
 * @author Libor Krzyzanek
 * 
 */
public class ContributorConverterTest {

	@Test
	public void testConvertToModel() throws IOException {
		ContributorConverter converter = new ContributorConverter();
		Map<String, Object> data = new HashMap<String, Object>();

		data.put("code", "John Doe<john@doe.com>");
		List<String> emails = new ArrayList<String>();
		emails.add("john@doe.com");
		data.put("email", emails);

		Contributor c = converter.convertToModel("john@doe.com", data);

		TestUtils.assertJsonContent("{\"code\": \"John Doe<john@doe.com>\", \"email\":[\"john@doe.com\"]}",
				c.getValue());
	}
}
