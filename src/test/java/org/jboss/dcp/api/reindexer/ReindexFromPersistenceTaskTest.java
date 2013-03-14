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

import org.jboss.dcp.api.DcpContentObjectFields;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SearchClientService;
import org.jboss.dcp.api.testtools.ESRealClientTestBase;
import org.jboss.dcp.persistence.service.ContentPersistenceService;
import org.jboss.dcp.persistence.service.ContentPersistenceService.ListRequest;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link ReindexFromPersistenceTask}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ReindexFromPersistenceTaskTest extends ESRealClientTestBase {

	String dcpContentType = "tt";
	String indexName = "myindex";
	String typeName = "mytype";

	@SuppressWarnings("unchecked")
	@Test
	public void run_ok() {

		try {
			ReindexFromPersistenceTask tested = new ReindexFromPersistenceTask();
			tested.searchClientService = Mockito.mock(SearchClientService.class);
			Mockito.when(tested.searchClientService.getClient()).thenReturn(prepareESClientForUnitTest());
			tested.dcpContentType = dcpContentType;
			tested.providerService = Mockito.mock(ProviderService.class);
			List<Map<String, Object>> preprocessorsDef = new ArrayList<Map<String, Object>>();

			configProviderServiceMock(tested, preprocessorsDef);

			indexCreate(indexName);
			indexMappingCreate(indexName, typeName, "{ \"" + typeName + "\" : {\"_timestamp\" : { \"enabled\" : true }}}");
			// case - put it into empty index
			{
				tested.contentPersistenceService = getContentPersistenceServiceMock(false);
				tested.run();
				indexFlush(indexName);
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-1"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-3"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-4"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-5"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-6"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-7"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-8"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-9"));
				Mockito.verify(tested.providerService, Mockito.times(8)).runPreprocessors(Mockito.eq(dcpContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
			}

			// case - put it into non empty index to check if records are deleted correctly
			{
				Mockito.reset(tested.providerService);
				configProviderServiceMock(tested, preprocessorsDef);
				tested.contentPersistenceService = getContentPersistenceServiceMock(true);
				tested.run();
				indexFlush(indexName);
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-1"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-2"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-3"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-4"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-5"));
				Assert.assertNotNull(indexGetDocument(indexName, typeName, "tt-6"));
				// next two must be removed from index because not in persistent store anymore
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-7"));
				Assert.assertNull(indexGetDocument(indexName, typeName, "tt-8"));
				Mockito.verify(tested.providerService, Mockito.times(6)).runPreprocessors(Mockito.eq(dcpContentType),
						Mockito.eq(preprocessorsDef), Mockito.anyMap());
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
		Mockito.when(tested.providerService.findContentType(dcpContentType)).thenReturn(typeDef);
	}

	private ContentPersistenceService getContentPersistenceServiceMock(boolean shorter) {
		ContentPersistenceService ret = Mockito.mock(ContentPersistenceService.class);

		TestListRequest listRequest1 = new TestListRequest();
		addContent(listRequest1, "tt-1");
		addContent(listRequest1, "tt-2");
		addContent(listRequest1, "tt-3");
		Mockito.when(ret.listRequestInit(dcpContentType)).thenReturn(listRequest1);

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
		content.put(DcpContentObjectFields.DCP_ID, id);
		content.put(DcpContentObjectFields.DCP_CONTENT_TYPE, dcpContentType);
		content.put(DcpContentObjectFields.DCP_DESCRIPTION, "value " + id);
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
