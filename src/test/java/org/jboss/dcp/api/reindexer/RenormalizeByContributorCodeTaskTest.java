/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.reindexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.elasticsearch.indices.IndexMissingException;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.tasker.TaskExecutionContext;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for {@link RenormalizeByContributorCodeTask}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByContributorCodeTaskTest extends ESRealClientTestBase {

	String contributorCode = "project 1";
	String contributorCode2 = "project 2";
	String dcpContentType = "tt";
	String indexName = "myindex";
	String typeName = "mytype";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void performTask_ok() throws Exception {

		try {
			RenormalizeByContributorCodeTask tested = new RenormalizeByContributorCodeTask();
			tested.searchClientService = Mockito.mock(SearchClientService.class);
			Mockito.when(tested.searchClientService.getClient()).thenReturn(prepareESClientForUnitTest());
			tested.contributorCodes = new String[] { contributorCode };
			tested.providerService = Mockito.mock(ProviderService.class);
			TaskExecutionContext contextMock = Mockito.mock(TaskExecutionContext.class);
			tested.setExecutionContext("tid", contextMock);
			List<Map<String, Object>> preprocessorsDef = new ArrayList<Map<String, Object>>();

			// case - run on nonexisting index
			try {
				configProviderServiceMock(tested, preprocessorsDef);
				tested.performTask();
				Assert.fail("IndexMissingException expected");
			} catch (IndexMissingException e) {
				Mockito.verify(tested.providerService, Mockito.times(0)).runPreprocessors(Mockito.eq(dcpContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
			}

			indexCreate(indexName);
			indexMappingCreate(indexName, typeName, "{ \"" + typeName
					+ "\" : {\"properties\": {\"dcp_contributors\" : {\"type\" : \"string\", \"analyzer\" : \"keyword\"}}}}");
			// case - run on empty index
			{
				Mockito.reset(tested.providerService);
				configProviderServiceMock(tested, preprocessorsDef);
				tested.performTask();
				indexFlush(indexName);
				Mockito.verify(tested.providerService, Mockito.times(0)).runPreprocessors(Mockito.eq(dcpContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
				Mockito.verify(contextMock).writeTaskLog(Mockito.anyString(), Mockito.eq("Processed 0 documents."));
			}

			// case - run on non empty index, for one project
			{
				indexInsertDocument(indexName, typeName, "tt-1",
						"{\"id\" : \"tt1\", \"dcp_contributors\" : [\"project 3\",\"project 1\"], \"dcp_content_type\" : \"tt\"}");
				indexInsertDocument(indexName, typeName, "tt-2",
						"{\"id\" : \"tt2\", \"dcp_contributors\" : \"project 1\", \"dcp_content_type\" : \"tt\"}");
				indexInsertDocument(indexName, typeName, "tt-3",
						"{\"id\" : \"tt3\", \"dcp_contributors\" : \"project 1\", \"dcp_content_type\" : \"tt\"}");
				indexInsertDocument(indexName, typeName, "tt-4",
						"{\"id\" : \"tt4\", \"dcp_contributors\" : \"project 2\", \"dcp_content_type\" : \"tt\"}");
				indexInsertDocument(indexName, typeName, "tt-5",
						"{\"id\" : \"tt5\", \"dcp_contributors\" : [\"project 1\",\"project 2\"], \"dcp_content_type\" : \"tt\"}");
				indexInsertDocument(indexName, typeName, "tt-6",
						"{\"id\" : \"tt6\", \"dcp_contributors\" : \"project 2\", \"dcp_content_type\" : \"tt\"}");

				// next must be skipped due nonexisting type definition
				indexInsertDocument(indexName, typeName, "tt-7",
						"{\"id\" : \"tt7\", \"dcp_contributors\" : \"project 1\", \"dcp_content_type\" : \"tt2\"}");
				indexInsertDocument(indexName, typeName, "tt-8",
						"{\"id\" : \"tt8\", \"dcp_contributors\" : \"project 3\", \"dcp_content_type\" : \"tt\"}");
				indexFlush(indexName);

				Mockito.reset(tested.providerService, contextMock);
				configProviderServiceMock(tested, preprocessorsDef);

				Mockito.doAnswer(new Answer() {

					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
						Map<String, Object> m = (Map<String, Object>) invocation.getArguments()[2];
						m.put("called", "yes");
						return null;
					}
				}).when(tested.providerService)
						.runPreprocessors(Mockito.eq(dcpContentType), Mockito.anyList(), Mockito.anyMap());

				tested.performTask();

				indexFlush(indexName);
				// check preprocessors was called for all documents in index
				Assert.assertEquals("yes", indexGetDocument(indexName, typeName, "tt-1").get("called"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-2").get("called"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-3").get("called"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-4").get("called"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-5").get("called"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-6").get("called"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-7").get("called"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-8").get("called"));
				Mockito.verify(tested.providerService, Mockito.times(4)).runPreprocessors(Mockito.eq(dcpContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
				Mockito.verify(contextMock).writeTaskLog(Mockito.anyString(),
						Mockito.eq("No type definition found for document id=tt-7 so is skipped"));
				Mockito.verify(contextMock).writeTaskLog(Mockito.anyString(), Mockito.eq("Processed 5 documents."));
			}

			// case - run on non empty index, for more projects
			{
				tested.contributorCodes = new String[] { contributorCode, contributorCode2 };

				Mockito.reset(tested.providerService, contextMock);
				configProviderServiceMock(tested, preprocessorsDef);

				Mockito.doAnswer(new Answer() {

					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
						Map<String, Object> m = (Map<String, Object>) invocation.getArguments()[2];
						m.put("called2", "yes");
						return null;
					}
				}).when(tested.providerService)
						.runPreprocessors(Mockito.eq(dcpContentType), Mockito.anyList(), Mockito.anyMap());

				tested.performTask();

				indexFlush(indexName);
				// check preprocessors was called for all documents in index
				Assert.assertEquals("yes", indexGetDocument(indexName, typeName, "tt-1").get("called2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-2").get("called2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-3").get("called2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-4").get("called2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-5").get("called2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-6").get("called2"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-7").get("called2"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-8").get("called2"));
				Mockito.verify(tested.providerService, Mockito.times(6)).runPreprocessors(Mockito.eq(dcpContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
				Mockito.verify(contextMock).writeTaskLog(Mockito.anyString(),
						Mockito.eq("No type definition found for document id=tt-7 so is skipped"));
				Mockito.verify(contextMock).writeTaskLog(Mockito.anyString(), Mockito.eq("Processed 7 documents."));
			}

		} finally {
			finalizeESClientForUnitTest();
		}
	}

	private void configProviderServiceMock(RenormalizeByContributorCodeTask tested,
			List<Map<String, Object>> preprocessorsDef) {
		Map<String, Object> typeDef = new HashMap<String, Object>();
		typeDef.put(ProviderService.INPUT_PREPROCESSORS, preprocessorsDef);
		Map<String, Object> index = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, index);
		index.put(ProviderService.NAME, indexName);
		index.put(ProviderService.TYPE, typeName);
		Mockito.when(tested.providerService.findContentType(dcpContentType)).thenReturn(typeDef);
		Mockito.when(tested.providerService.findContentType("tt2")).thenReturn(null);

		Map<String, Object> providerDef = new HashMap<String, Object>();
		Map<String, Object> typesDef = new HashMap<String, Object>();
		providerDef.put(ProviderService.TYPE, typesDef);
		typesDef.put(dcpContentType, typeDef);
		List<Map<String, Object>> providers = new ArrayList<Map<String, Object>>();
		providers.add(providerDef);
		Mockito.when(tested.providerService.getAll()).thenReturn(providers);
	}

}
