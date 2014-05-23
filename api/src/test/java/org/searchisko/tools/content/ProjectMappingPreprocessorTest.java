/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.tools.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.jboss.elasticsearch.tools.content.ESLookupValuePreprocessor;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.service.ProjectService;

/**
 * Unit test for {@link ProjectMappingPreprocessor}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ProjectMappingPreprocessorTest {

	@Test
	public void init_defaults() {
		ProjectMappingPreprocessor tested = new ProjectMappingPreprocessor();
		Client clientMock = Mockito.mock(Client.class);
		Map<String, Object> settings = new HashMap<>();
		settings.put("source_field", "sf");
		settings.put("idx_search_field", "if");

		tested.init("Project mapper", clientMock, settings);

		Assert.assertTrue(tested instanceof ESLookupValuePreprocessor);

		Assert.assertEquals("Project mapper", tested.getName());

		Assert.assertEquals(ProjectService.SEARCH_INDEX_NAME, tested.getIndexName());
		Assert.assertEquals(ProjectService.SEARCH_INDEX_TYPE, tested.getIndexType());

		Assert.assertEquals(1, tested.getIdxSearchField().size());
		Assert.assertTrue(tested.getIdxSearchField().contains("if"));
		Assert.assertEquals("sf", tested.getSourceField());
		Assert.assertEquals(null, tested.getSourceValuePattern());

		List<Map<String, String>> mr = tested.getResultMapping();
		Assert.assertEquals(2, mr.size());
	}

	@Test
	public void init_resultMappingOverwrite() {
		ProjectMappingPreprocessor tested = new ProjectMappingPreprocessor();
		Client clientMock = Mockito.mock(Client.class);
		Map<String, Object> settings = new HashMap<>();
		settings.put("source_value", "sv");
		settings.put("idx_search_field", "if");

		List<Map<String, String>> rmMock = new ArrayList<>();
		settings.put("result_mapping", rmMock);
		Map<String, String> rm_code = new HashMap<>();
		rm_code.put("idx_result_field", ProjectService.FIELD_CODE);
		rm_code.put("target_field", ContentObjectFields.SYS_PROJECT);
		rmMock.add(rm_code);

		tested.init("Project mapper", clientMock, settings);

		Assert.assertTrue(tested instanceof ESLookupValuePreprocessor);

		Assert.assertEquals("Project mapper", tested.getName());

		Assert.assertEquals(ProjectService.SEARCH_INDEX_NAME, tested.getIndexName());
		Assert.assertEquals(ProjectService.SEARCH_INDEX_TYPE, tested.getIndexType());

		Assert.assertEquals(1, tested.getIdxSearchField().size());
		Assert.assertTrue(tested.getIdxSearchField().contains("if"));
		Assert.assertEquals(null, tested.getSourceField());
		Assert.assertEquals("sv", tested.getSourceValuePattern());

		List<Map<String, String>> mr = tested.getResultMapping();
		Assert.assertEquals(rmMock, mr);
	}

}
