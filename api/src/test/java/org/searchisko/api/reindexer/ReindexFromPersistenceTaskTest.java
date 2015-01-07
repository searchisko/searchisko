/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.reindexer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;

import org.hamcrest.CustomMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderServiceTest;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.tasker.TaskExecutionContext;
import org.searchisko.api.testtools.ESRealClientTestBase;
import org.searchisko.persistence.service.ContentPersistenceService;
import org.searchisko.persistence.service.ContentTuple;
import org.searchisko.persistence.service.ListRequest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
			tested.setExecutionContext("tid", Mockito.mock(TaskExecutionContext.class));
			tested.searchClientService = Mockito.mock(SearchClientService.class);
			Mockito.when(tested.searchClientService.getClient()).thenReturn(prepareESClientForUnitTest("ReindexFromPersistenceTaskTest"));
			Mockito.doCallRealMethod().when(tested.searchClientService)
					.performDeleteOldRecords(Mockito.anyString(), Mockito.anyString(), Mockito.any(Date.class));
			tested.sysContentType = sysContentType;
			tested.providerService = Mockito.mock(ProviderService.class);
			tested.eventBeforeIndexed = Mockito.mock(Event.class);
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
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-1"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-2"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-3"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-4"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-5"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-6"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-7"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-8"));
				verifyNoMoreInteractions(tested.eventBeforeIndexed);
			}

			// case - put it into non empty index to check if records are deleted correctly
			{
				Mockito.reset(tested.providerService, tested.eventBeforeIndexed);
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
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-1"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-2"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-3"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-4"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-5"));
				verify(tested.eventBeforeIndexed).fire(prepareContentBeforeIndexedEventMatcher("tt-6"));
				verifyNoMoreInteractions(tested.eventBeforeIndexed);

			}
		} finally {
			finalizeESClientForUnitTest();
		}
	}

	private ContentBeforeIndexedEvent prepareContentBeforeIndexedEventMatcher(final String expectedId) {
		return Mockito.argThat(new CustomMatcher<ContentBeforeIndexedEvent>("ContentBeforeIndexedEvent [contentId="
				+ expectedId + "]") {

			@Override
			public boolean matches(Object paramObject) {
				ContentBeforeIndexedEvent e = (ContentBeforeIndexedEvent) paramObject;
				return e.getContentId().equals(expectedId) && e.getContentData() != null;
			}

		});
	}

	private void configProviderServiceMock(ReindexFromPersistenceTask tested, List<Map<String, Object>> preprocessorsDef) {
		Map<String, Object> typeDef = new HashMap<String, Object>();
		typeDef.put(ProviderService.INPUT_PREPROCESSORS, preprocessorsDef);
		Map<String, Object> index = new HashMap<String, Object>();
		typeDef.put(ProviderService.INDEX, index);
		index.put(ProviderService.NAME, indexName);
		index.put(ProviderService.TYPE, typeName);

		Mockito.when(tested.providerService.findContentType(sysContentType)).thenReturn(
				ProviderServiceTest.createProviderContentTypeInfo(typeDef));
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
		listRequest1.content.add(new ContentTuple<String, Map<String, Object>>(id, content));
	}

	protected static class TestListRequest implements ListRequest {

		List<ContentTuple<String, Map<String, Object>>> content = new ArrayList<>();

		@Override
		public boolean hasContent() {
			return content != null && !content.isEmpty();
		}

		@Override
		public List<ContentTuple<String, Map<String, Object>>> content() {
			return content;
		}

	}

}
