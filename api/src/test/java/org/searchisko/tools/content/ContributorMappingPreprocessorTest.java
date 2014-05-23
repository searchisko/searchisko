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
import org.searchisko.api.service.ContributorService;

/**
 * Unit test for {@link ContributorMappingPreprocessor}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ContributorMappingPreprocessorTest {

	@Test
	public void init_defaults() {
		ContributorMappingPreprocessor tested = new ContributorMappingPreprocessor();
		Client clientMock = Mockito.mock(Client.class);
		Map<String, Object> settings = new HashMap<>();
		settings.put("source_field", "sf");
		settings.put("idx_search_field", "if");

		tested.init("Contributor mapper", clientMock, settings);

		Assert.assertTrue(tested instanceof ESLookupValuePreprocessor);

		Assert.assertEquals("Contributor mapper", tested.getName());

		Assert.assertEquals(ContributorService.SEARCH_INDEX_NAME, tested.getIndexName());
		Assert.assertEquals(ContributorService.SEARCH_INDEX_TYPE, tested.getIndexType());

		Assert.assertEquals(1, tested.getIdxSearchField().size());
		Assert.assertTrue(tested.getIdxSearchField().contains("if"));
		Assert.assertEquals("sf", tested.getSourceField());
		Assert.assertEquals(null, tested.getSourceValuePattern());

		List<Map<String, String>> mr = tested.getResultMapping();
		Assert.assertEquals(1, mr.size());
	}

	@Test
	public void init_resultMappingOverwrite() {
		ContributorMappingPreprocessor tested = new ContributorMappingPreprocessor();
		Client clientMock = Mockito.mock(Client.class);
		Map<String, Object> settings = new HashMap<>();
		settings.put("source_value", "sv");
		settings.put("idx_search_field", "if");

		List<Map<String, String>> rmMock = new ArrayList<>();
		settings.put("result_mapping", rmMock);
		Map<String, String> rm_code = new HashMap<>();
		rm_code.put("idx_result_field", ContributorService.FIELD_CODE);
		rm_code.put("target_field", ContentObjectFields.SYS_PROJECT);
		rmMock.add(rm_code);

		tested.init("Contributor mapper", clientMock, settings);

		Assert.assertTrue(tested instanceof ESLookupValuePreprocessor);

		Assert.assertEquals("Contributor mapper", tested.getName());

		Assert.assertEquals(ContributorService.SEARCH_INDEX_NAME, tested.getIndexName());
		Assert.assertEquals(ContributorService.SEARCH_INDEX_TYPE, tested.getIndexType());

		Assert.assertEquals(1, tested.getIdxSearchField().size());
		Assert.assertTrue(tested.getIdxSearchField().contains("if"));
		Assert.assertEquals(null, tested.getSourceField());
		Assert.assertEquals("sv", tested.getSourceValuePattern());

		List<Map<String, String>> mr = tested.getResultMapping();
		Assert.assertEquals(rmMock, mr);
	}

}
