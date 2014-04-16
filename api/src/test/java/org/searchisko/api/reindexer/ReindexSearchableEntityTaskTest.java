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

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.service.SearchableEntityService;
import org.searchisko.api.tasker.TaskExecutionContext;
import org.searchisko.persistence.service.ContentTuple;
import org.searchisko.persistence.service.ListRequest;

/**
 * Unit test for {@link ReindexSearchableEntityTask}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class ReindexSearchableEntityTaskTest {

	@Test
	public void performTask_empty() throws Exception {

		SearchableEntityService sesMock = Mockito.mock(SearchableEntityService.class);
		ReindexSearchableEntityTask tested = new ReindexSearchableEntityTask(sesMock);
		tested.setExecutionContext("t", Mockito.mock(TaskExecutionContext.class));

		Mockito.when(sesMock.listRequestInit()).thenReturn(new TestListRequest());

		tested.performTask();

		Mockito.verify(sesMock).listRequestInit();
		Mockito.verifyNoMoreInteractions(sesMock);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void performTask_morePages() throws Exception {

		SearchableEntityService sesMock = Mockito.mock(SearchableEntityService.class);
		ReindexSearchableEntityTask tested = new ReindexSearchableEntityTask(sesMock);
		tested.setExecutionContext("t", Mockito.mock(TaskExecutionContext.class));

		final List<BulkRequestBuilder> brbMocks = new ArrayList<>();
		final List<ListenableActionFuture<BulkResponse>> brespMocks = new ArrayList<>();

		Mockito.when(sesMock.prepareBulkRequest()).then(new Answer<BulkRequestBuilder>() {

			@Override
			public BulkRequestBuilder answer(InvocationOnMock invocation) throws Throwable {
				BulkRequestBuilder brbMock = Mockito.mock(BulkRequestBuilder.class);
				brbMocks.add(brbMock);

				ListenableActionFuture<BulkResponse> brespMock = Mockito.mock(ListenableActionFuture.class);
				Mockito.when(brbMock.execute()).thenReturn(brespMock);
				brespMocks.add(brespMock);

				return brbMock;
			}

		});

		TestListRequest tlsFirst = new TestListRequest();
		HashMap<String, Object> content1 = new HashMap<String, Object>();
		tlsFirst.content.add(new ContentTuple<String, Map<String, Object>>("id1", content1));
		HashMap<String, Object> content2 = new HashMap<String, Object>();
		tlsFirst.content.add(new ContentTuple<String, Map<String, Object>>("id2", content2));
		Mockito.when(sesMock.listRequestInit()).thenReturn(tlsFirst);

		TestListRequest tlsSecond = new TestListRequest();
		HashMap<String, Object> content21 = new HashMap<String, Object>();
		tlsSecond.content.add(new ContentTuple<String, Map<String, Object>>("id21", content21));
		HashMap<String, Object> content22 = new HashMap<String, Object>();
		tlsSecond.content.add(new ContentTuple<String, Map<String, Object>>("id22", content22));
		Mockito.when(sesMock.listRequestNext(tlsFirst)).thenReturn(tlsSecond);

		Mockito.when(sesMock.listRequestNext(tlsSecond)).thenReturn(new TestListRequest());

		tested.performTask();

		Mockito.verify(sesMock).listRequestInit();
		Mockito.verify(sesMock, Mockito.times(2)).prepareBulkRequest();
		Mockito.verify(sesMock).updateSearchIndex(brbMocks.get(0), "id1", content1);
		Mockito.verify(sesMock).updateSearchIndex(brbMocks.get(0), "id2", content2);
		Mockito.verify(sesMock).updateSearchIndex(brbMocks.get(1), "id21", content21);
		Mockito.verify(sesMock).updateSearchIndex(brbMocks.get(1), "id22", content22);
		Mockito.verify(sesMock).listRequestNext(tlsFirst);
		Mockito.verify(sesMock).listRequestNext(tlsSecond);
		Mockito.verify(sesMock).deleteOldFromSearchIndex(Mockito.notNull(Date.class));
		Mockito.verifyNoMoreInteractions(sesMock);

		for (BulkRequestBuilder brbMock : brbMocks) {
			Mockito.verify(brbMock).execute();
		}
		for (ListenableActionFuture<BulkResponse> brespMock : brespMocks) {
			Mockito.verify(brespMock).actionGet();
		}

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
