/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.ContentTuple;

/**
 * Unit test for {@link ContributorConverter}
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
public class ContributorConverterTest {

	@Test
	public void convertToModel() throws IOException {
		ContributorConverter converter = new ContributorConverter();
		Map<String, Object> data = new LinkedHashMap<String, Object>();

		data.put("code", "John Doe<john@doe.com>");
		List<String> emails = new ArrayList<String>();
		emails.add("john@doe.com");
		data.put("email", emails);
		data.put("name", "John Doé");

		Contributor c = converter.convertToModel("10", data);

		TestUtils.assertJsonContent(
				"{\"code\": \"John Doe<john@doe.com>\", \"email\":[\"john@doe.com\"], \"name\": \"John Doé\"}", c.getValue());

	}

	@Test
	public void convertToContentTuple() throws IOException {
		ContributorConverter tested = new ContributorConverter();

		Contributor jpaEntity = new Contributor();
		jpaEntity.setId("myid");
		jpaEntity.setValue("{\"code\": \"John Doe<john@doe.com>\"}");
		ContentTuple<String, Map<String, Object>> result = tested.convertToContentTuple(jpaEntity);
		Assert.assertEquals("myid", result.getId());
		Assert.assertNotNull(result.getContent());
		Assert.assertEquals("John Doe<john@doe.com>", result.getContent().get("code"));
	}
}
