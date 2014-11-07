/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.jboss.elasticsearch.tools.content.InvalidDataException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.rest.exception.PreprocessorInvalidDataException;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderServiceTest;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.tasker.TaskExecutionContext;
import org.searchisko.api.testtools.ESRealClientTestBase;

/**
 * Unit test for {@link RenormalizeByContentTypeTask}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class RenormalizeByContentTypeTaskTest extends ESRealClientTestBase {

	String sysContentType = "tt";
	String indexName = "myindex";
	String typeName = "mytype";

	@SuppressWarnings("unchecked")
	@Test
	public void performHitProcessing_PreprocessorInvalidDataException() throws Exception {
		RenormalizeByContentTypeTask tested = new RenormalizeByContentTypeTask();
		tested.sysContentType = sysContentType;
		tested.providerService = Mockito.mock(ProviderService.class);
		TaskExecutionContext context = Mockito.mock(TaskExecutionContext.class);
		tested.setExecutionContext("tid", context);

		List<Map<String, Object>> preprocessorsDef = new ArrayList<Map<String, Object>>();
		configProviderServiceMock(tested, preprocessorsDef);
		tested.validateTaskConfiguration();

		SearchHit hit = Mockito.mock(SearchHit.class);
		BulkRequestBuilder brb = Mockito.mock(BulkRequestBuilder.class);
		Client client = Mockito.mock(Client.class);

		Mockito.when(hit.getId()).thenReturn("hitid");
		Mockito.when(
				tested.providerService.runPreprocessors(Mockito.eq(sysContentType), Mockito.anyList(), Mockito.anyMap()))
				.thenThrow(new PreprocessorInvalidDataException(new InvalidDataException("data error")));

		tested.performHitProcessing(client, brb, hit);

		Mockito.verify(context).writeTaskLog("tid",
				"ERROR: Data error from preprocessors execution so document hitid is skipped: data error");
		Mockito.verifyNoMoreInteractions(context, brb, client);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void performTask_ok() throws Exception {

		try {
			RenormalizeByContentTypeTask tested = new RenormalizeByContentTypeTask();
			tested.searchClientService = Mockito.mock(SearchClientService.class);
			Mockito.when(tested.searchClientService.getClient()).thenReturn(prepareESClientForUnitTest());
			tested.sysContentType = sysContentType;
			tested.providerService = Mockito.mock(ProviderService.class);
			tested.setExecutionContext("tid", Mockito.mock(TaskExecutionContext.class));
			List<Map<String, Object>> preprocessorsDef = new ArrayList<Map<String, Object>>();

			// case - run on nonexisting index
			indexDelete(indexName);
			try {
				configProviderServiceMock(tested, preprocessorsDef);
				tested.performTask();
				Assert.fail("IndexMissingException expected");
			} catch (IndexMissingException e) {
				Mockito.verify(tested.providerService, Mockito.times(1)).findContentType(sysContentType);
				Mockito.verify(tested.providerService, Mockito.times(0)).runPreprocessors(Mockito.eq(sysContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
			}

			indexCreate(indexName);
			indexMappingCreate(indexName, typeName, "{ \"" + typeName + "\" : {\"_timestamp\" : { \"enabled\" : true }}}");
			// case - run on empty index
			{
				Mockito.reset(tested.providerService);
				configProviderServiceMock(tested, preprocessorsDef);
				tested.performTask();
				indexFlushAndRefresh(indexName);
				Mockito.verify(tested.providerService, Mockito.times(1)).findContentType(sysContentType);
				Mockito.verify(tested.providerService, Mockito.times(0)).runPreprocessors(Mockito.eq(sysContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
			}

			// case - run on non empty index
			{
				indexInsertDocument(indexName, typeName, "tt-1", "{\"id\" : \"tt1\"}");
				indexInsertDocument(indexName, typeName, "tt-2", "{\"id\" : \"tt2\"}");
				indexInsertDocument(indexName, typeName, "tt-3", "{\"id\" : \"tt3\"}");
				indexInsertDocument(indexName, typeName, "tt-4", "{\"id\" : \"tt4\"}");
				indexInsertDocument(indexName, typeName, "tt-5", "{\"id\" : \"tt5\"}");
				indexInsertDocument(indexName, typeName, "tt-6", "{\"id\" : \"tt6\"}");
				indexFlushAndRefresh(indexName);

				Mockito.reset(tested.providerService);
				configProviderServiceMock(tested, preprocessorsDef);

				// prepare map of all ids from all document content so we can check preprocessor was called for each of them
				final Set<String> s = new HashSet<String>();
				s.addAll(Arrays.asList(new String[] { "tt1", "tt2", "tt3", "tt4", "tt5", "tt6" }));
				Mockito.doAnswer(new Answer() {

					@Override
					public Object answer(InvocationOnMock invocation) throws Throwable {
						Map<String, Object> m = (Map<String, Object>) invocation.getArguments()[2];
						s.remove(m.get("id"));
						m.put("called", "yes");
						return null;
					}
				}).when(tested.providerService)
						.runPreprocessors(Mockito.eq(sysContentType), Mockito.anyList(), Mockito.anyMap());

				tested.performTask();

				indexFlushAndRefresh(indexName);
				// check preprocessors was called for all documents in index
				Assert.assertTrue("May be empty but is " + s, s.isEmpty());
				Assert.assertEquals("yes", indexGetDocument(indexName, typeName, "tt-1").get("called"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-2").get("called"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-3").get("called"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-4").get("called"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-5").get("called"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-6").get("called"));
				Mockito.verify(tested.providerService, Mockito.times(6)).runPreprocessors(Mockito.eq(sysContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
			}
		} finally {
			finalizeESClientForUnitTest();
		}
	}

	private void configProviderServiceMock(RenormalizeByContentTypeTask tested, List<Map<String, Object>> preprocessorsDef) {
		Map<String, Object> typeDef = new HashMap<String, Object>();
		typeDef.put(ProviderService.INPUT_PREPROCESSORS, preprocessorsDef);
		Map<String, Object> index = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, index);
		index.put(ProviderService.NAME, indexName);
		index.put(ProviderService.TYPE, typeName);
		Mockito.when(tested.providerService.findContentType(sysContentType)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDef));
	}

}
