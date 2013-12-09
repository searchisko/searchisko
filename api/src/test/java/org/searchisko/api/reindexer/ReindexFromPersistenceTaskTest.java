/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.service.ContentEnhancementsService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.persistence.service.ContentPersistenceService;
import org.searchisko.persistence.service.ContentPersistenceService.ListRequest;

/**
 * Unit test for {@link ReindexFromPersistenceTask}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ReindexFromPersistenceTaskTest extends ESRealClientTestBase {

	String sysContentType = "tt";
	String indexName = "myindex";
	String typeName = "mytype";

	@SuppressWarnings("unchecked")
	@Test
	public void performTask_ok() throws Exception {

		try {
			ReindexFromPersistenceTask tested = new ReindexFromPersistenceTask();
			tested.searchClientService = Mockito.mock(SearchClientService.class);
			tested.contentEnhancementsService = Mockito.mock(ContentEnhancementsService.class);
			Mockito.when(tested.searchClientService.getClient()).thenReturn(prepareESClientForUnitTest());
			tested.sysContentType = sysContentType;
			tested.providerService = Mockito.mock(ProviderService.class);
			List<Map<String, Object>> preprocessorsDef = new ArrayList<Map<String, Object>>();

			configProviderServiceMock(tested, preprocessorsDef);

			indexDelete(indexName);
			indexCreate(indexName);
			indexMappingCreate(indexName, typeName, "{ \"" + typeName + "\" : {\"_timestamp\" : { \"enabled\" : true }}}");
			// case - put it into empty index
			{
				tested.contentPersistenceService = getContentPersistenceServiceMock(false);
				tested.performTask();
				indexFlushAndRefresh(indexName);
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-1"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-3"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-4"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-5"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-6"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-7"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-8"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-9"));
				Mockito.verify(tested.providerService, Mockito.times(8)).runPreprocessors(Mockito.eq(sysContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
				Mockito.verify(tested.contentEnhancementsService, Mockito.times(8)).handleContentRatingFields(Mockito.anyMap(),
						Mockito.anyString());
				Mockito.verify(tested.contentEnhancementsService, Mockito.times(8)).handleExternalTags(Mockito.anyMap(),
						Mockito.anyString());

			}

			// case - put it into non empty index to check if records are deleted correctly
			{
				Mockito.reset(tested.providerService, tested.contentEnhancementsService);
				configProviderServiceMock(tested, preprocessorsDef);
				tested.contentPersistenceService = getContentPersistenceServiceMock(true);
				tested.performTask();
				indexFlushAndRefresh(indexName);
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-1"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-3"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-4"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-5"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-6"));
				// next two must be removed from index because not in persistent store anymore
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-7"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-8"));
				Mockito.verify(tested.providerService, Mockito.times(6)).runPreprocessors(Mockito.eq(sysContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
				Mockito.verify(tested.contentEnhancementsService, Mockito.times(6)).handleContentRatingFields(Mockito.anyMap(),
						Mockito.anyString());
			}
		} finally {
			finalizeESClientForUnitTest();
		}
	}

	private void configProviderServiceMock(ReindexFromPersistenceTask tested, List<Map<String, Object>> preprocessorsDef) {
		Map<String, Object> typeDef = new HashMap<String, Object>();
		typeDef.put(ProviderService.INPUT_PREPROCESSORS, preprocessorsDef);
		Map<String, Object> index = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, index);
		index.put(ProviderService.NAME, indexName);
		index.put(ProviderService.TYPE, typeName);
		Mockito.when(tested.providerService.findContentType(sysContentType)).thenReturn(typeDef);
	}

	private ContentPersistenceService getContentPersistenceServiceMock(boolean shorter) {
		ContentPersistenceService ret = Mockito.mock(ContentPersistenceService.class);

		TestListRequest listRequest1 = new TestListRequest();
		addContent(listRequest1, "tt-1");
		addContent(listRequest1, "tt-2");
		addContent(listRequest1, "tt-3");
		Mockito.when(ret.listRequestInit(sysContentType)).thenReturn(listRequest1);

		TestListRequest listRequest2 = new TestListRequest();
		addContent(listRequest2, "tt-4");
		addContent(listRequest2, "tt-5");
		addContent(listRequest2, "tt-6");
		Mockito.when(ret.listRequestNext(listRequest1)).thenReturn(listRequest2);

		if (!shorter) {
			TestListRequest listRequest3 = new TestListRequest();
			addContent(listRequest3, "tt-7");
			addContent(listRequest3, "tt-8");
			Mockito.when(ret.listRequestNext(listRequest2)).thenReturn(listRequest3);
			Mockito.when(ret.listRequestNext(listRequest3)).thenReturn(new TestListRequest());
		} else {
			Mockito.when(ret.listRequestNext(listRequest2)).thenReturn(new TestListRequest());
		}

		return ret;
	}

	private void addContent(TestListRequest listRequest1, String id) {
		Map<String, Object> content = new HashMap<String, Object>();
		content.put(ContentObjectFields.SYS_ID, id);
		content.put(ContentObjectFields.SYS_CONTENT_TYPE, sysContentType);
		content.put(ContentObjectFields.SYS_DESCRIPTION, "value " + id);
		listRequest1.content.add(content);
	}

	protected static class TestListRequest implements ListRequest {

		List<Map<String, Object>> content = new ArrayList<Map<String, Object>>();

		@Override
		public boolean hasContent() {
			return content != null && !content.isEmpty();
		}

		@Override
		public List<Map<String, Object>> content() {
			return content;
		}

	}

}
