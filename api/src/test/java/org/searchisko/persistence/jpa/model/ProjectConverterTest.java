/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.persistence.jpa.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.junit.Assert;
import org.junit.Test;
import org.searchisko.api.service.ProjectService;
import org.searchisko.api.testtools.TestUtils;
import org.searchisko.persistence.service.ContentTuple;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link ProjectConverter}.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
public class ProjectConverterTest {

	@Test
	public void convertToModel() throws IOException {
		ProjectConverter converter = new ProjectConverter();

		{
			Map<String, Object> data = new HashMap<String, Object>();
			data.put(ProjectService.FIELD_CODE, "as7");
			data.put("name", "AS 7");

			Project p = converter.convertToModel("as7", data);

			assertEquals("as7", p.getCode());
			TestUtils.assertJsonContent("{\"name\":\"AS 7\",\"" + ProjectService.FIELD_CODE + "\":\"as7\"}", p.getValue());
		}

		{
			Map<String, Object> data = new HashMap<String, Object>();
			Project p = converter.convertToModel("as7", data);
			assertEquals("as7", p.getCode());
			assertEquals("{}", p.getValue());
		}

		{
			Project p = converter.convertToModel("as7", null);
			assertEquals("as7", p.getCode());
			assertEquals("", p.getValue());
		}

	}

	@Test
	public void convertToContentTuple() throws IOException {
		ProjectConverter tested = new ProjectConverter();

		Project jpaEntity = new Project();
		jpaEntity.setCode("myid");
		jpaEntity.setValue("{\"code\": \"John Doe<john@doe.com>\"}");
		ContentTuple<String, Map<String, Object>> result = tested.convertToContentTuple(jpaEntity);
		Assert.assertEquals("myid", result.getId());
		Assert.assertNotNull(result.getContent());
		Assert.assertEquals("John Doe<john@doe.com>", result.getContent().get("code"));
	}

	@Test(expected = JsonParseException.class)
	public void convertToContentTuple_invalidContent() throws IOException {
		ProjectConverter tested = new ProjectConverter();
		Project jpaEntity = new Project();
		jpaEntity.setCode("myid");
		jpaEntity.setValue("{\"code}");
		tested.convertToContentTuple(jpaEntity);
	}

	@Test
	public void convertToContentTuple_emptyContent() throws IOException {
		ProjectConverter tested = new ProjectConverter();
		Project jpaEntity = new Project();
		jpaEntity.setCode("myid");
		jpaEntity.setValue("");
		ContentTuple<String, Map<String, Object>> result = tested.convertToContentTuple(jpaEntity);
		Assert.assertEquals("myid", result.getId());
		Assert.assertNull(result.getContent());
	}

	@Test
	public void convertToContentTuple_nullContent() throws IOException {
		ProjectConverter tested = new ProjectConverter();
		Project jpaEntity = new Project();
		jpaEntity.setCode("myid");
		jpaEntity.setValue(null);
		ContentTuple<String, Map<String, Object>> result = tested.convertToContentTuple(jpaEntity);
		Assert.assertEquals("myid", result.getId());
		Assert.assertNull(result.getContent());
	}

	@Test
	public void convertToContentTuple_emptyJsonContent() throws IOException {
		ProjectConverter tested = new ProjectConverter();
		Project jpaEntity = new Project();
		jpaEntity.setCode("myid");
		jpaEntity.setValue("{}");
		ContentTuple<String, Map<String, Object>> result = tested.convertToContentTuple(jpaEntity);
		Assert.assertEquals("myid", result.getId());
		Assert.assertNotNull(result.getContent());
		Assert.assertTrue(result.getContent().isEmpty());
	}

}
