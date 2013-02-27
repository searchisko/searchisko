/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Logger;

import org.apache.lucene.document.DateTools;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.Client;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.jboss.dcp.api.config.StatsConfiguration;
import org.jboss.dcp.api.config.TimeoutConfiguration;
import org.jboss.dcp.api.model.AppConfiguration;
import org.jboss.dcp.api.model.AppConfiguration.ClientType;
import org.jboss.dcp.api.model.FacetValue;
import org.jboss.dcp.api.model.PastIntervalValue;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.Filters;
import org.jboss.dcp.api.model.SortByValue;
import org.jboss.dcp.api.testtools.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for {@link StatsClientService}
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StatsClientServiceTest {

	@Test
	public void init_embedded() throws Exception {
		AppConfigurationService acs = new AppConfigurationService();
		AppConfiguration ac = new AppConfiguration();
		acs.appConfiguration = ac;
		ac.setClientType(ClientType.EMBEDDED);
		ac.setProviderCreateInitData(false);
		ac.setAppDataPath(System.getProperty("java.io.tmpdir"));

		StatsClientService tested = new StatsClientService();
		tested.appConfigurationService = acs;
		tested.log = Logger.getLogger("testlogger");

		try {
			tested.init();
			Assert.assertNotNull(tested.node);
			Assert.assertNotNull(tested.client);
			Assert.assertNotNull(tested.statsLogListener);
		} finally {
			tested.destroy();
			Assert.assertNull(tested.node);
			Assert.assertNull(tested.client);
		}
	}

	@Test
	public void init_transport() {
		// untestable :-(
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeStatistics_Exception() throws ParseException, IOException {
		final StatsClientService tested = new StatsClientService();
		tested.client = Mockito.mock(Client.class);
		tested.log = Logger.getLogger("testlogger");
		tested.statsLogListener = Mockito.mock(ActionListener.class);
		tested.timeout = Mockito.mock(TimeoutConfiguration.class);
		Mockito.when(tested.timeout.stats()).thenReturn(256);

		// case - statistics disabled
		QuerySettings qs = new QuerySettings();
		qs.setQuery("my query");
		tested.statsConfiguration = new StatsConfiguration(false);
		tested.writeStatistics(StatsRecordType.SEARCH, new ElasticSearchException("Test exception"),
				DateTools.stringToTime("20121221121212121"), qs);
		Mockito.verifyZeroInteractions(tested.client);

		// case - statistics enabled, no filter defined
		qs.setFilters(new Filters());
		Mockito.reset(tested.client);
		tested.statsConfiguration = new StatsConfiguration(true);
		{
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_exception_noquery.json"));
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatistics(StatsRecordType.SEARCH, new ElasticSearchException("Test exception"),
					DateTools.stringToTime("20121221121212121"), qs);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			answ.assertError();
			Mockito.verifyNoMoreInteractions(tested.client);
		}

		// case - statistics enabled, search settings defined
		Mockito.reset(tested.client);
		qs.setQuery("my query");
		qs.setQueryHighlight(true);
		qs.addFacet(FacetValue.ACTIVITY_DATES_HISTOGRAM);
		qs.addFacet(FacetValue.TAG_CLOUD);
		qs.addField("_id");
		qs.addField("_source");
		qs.setSortBy(SortByValue.NEW);
		Filters filters = new Filters();
		qs.setFilters(filters);
		filters.addContributor("John Doe <john@doe.org>");
		filters.addContributor("Bill Hur <bill@hur.eu>");
		filters.addDcpType("issue");
		filters.addDcpType("blogpost");
		filters.addProject("jbossas7");
		filters.addProject("jbpm");
		filters.addTag("tag1");
		filters.addTag("tag2");
		filters.setActivityDateFrom(654653265465l);
		filters.setActivityDateTo(654653365465l);
		filters.setActivityDateInterval(PastIntervalValue.QUARTER);
		filters.setContentType("jbossorg_jira_issue");
		filters.setDcpContentProvider("jbossorg");
		filters.setFrom(128);
		filters.setSize(12);
		{
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_exception_query.json"));
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatistics(StatsRecordType.SEARCH, new ElasticSearchException("Test exception"),
					DateTools.stringToTime("20121221121212121"), qs);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			answ.assertError();
			Mockito.verifyNoMoreInteractions(tested.client);
		}

		// case - exception from elasticsearch call is not thrown out of write method
		Mockito.reset(tested.client);
		tested.statsConfiguration = new StatsConfiguration(true);
		Mockito.doThrow(new RuntimeException("testException")).when(tested.client)
				.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

		tested.writeStatistics(StatsRecordType.SEARCH, new ElasticSearchException("Test exception"),
				DateTools.stringToTime("20121221121212121"), qs);

		Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
		Mockito.verifyNoMoreInteractions(tested.client);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeStatistics_OK() throws ParseException, IOException {
		final StatsClientService tested = new StatsClientService();
		tested.client = Mockito.mock(Client.class);
		tested.log = Logger.getLogger("testlogger");
		tested.statsLogListener = Mockito.mock(ActionListener.class);
		tested.timeout = Mockito.mock(TimeoutConfiguration.class);
		Mockito.when(tested.timeout.stats()).thenReturn(256);

		SearchResponse searchResponseMock = Mockito.mock(SearchResponse.class);

		// case - statistics disabled
		QuerySettings qs = new QuerySettings();

		tested.statsConfiguration = new StatsConfiguration(false);
		tested.writeStatistics(StatsRecordType.SEARCH, "uuid", searchResponseMock,
				DateTools.stringToTime("20121221121212121"), qs);
		Mockito.verifyZeroInteractions(tested.client);

		// case - search response null
		tested.statsConfiguration = new StatsConfiguration(true);
		tested.writeStatistics(StatsRecordType.SEARCH, "uuid", (SearchResponse) null,
				DateTools.stringToTime("20121221121212121"), qs);
		Mockito.verifyZeroInteractions(tested.client);

		// case - statistics enabled, empty search settings
		Mockito.reset(tested.client);
		tested.statsConfiguration = new StatsConfiguration(true);
		SearchHits hitsMock = Mockito.mock(SearchHits.class);
		SearchHit[] hitsValues = new SearchHit[5];
		for (int i = 0; i < hitsValues.length; i++) {
			hitsValues[i] = new InternalSearchHit(i, "idx" + i, "type", null, null);
		}
		Mockito.when(hitsMock.totalHits()).thenReturn(129l);
		Mockito.when(hitsMock.maxScore()).thenReturn(29.3f);
		Mockito.when(hitsMock.hits()).thenReturn(hitsValues);
		Mockito.when(searchResponseMock.hits()).thenReturn(hitsMock);
		Mockito.when(searchResponseMock.status()).thenReturn(RestStatus.OK);
		Mockito.when(searchResponseMock.tookInMillis()).thenReturn(456l);
		Mockito.when(searchResponseMock.successfulShards()).thenReturn(2);
		Mockito.when(searchResponseMock.failedShards()).thenReturn(1);
		Mockito.when(searchResponseMock.getShardFailures()).thenReturn(
				new ShardSearchFailure[] { new ShardSearchFailure("test reason", null) });

		{
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_ok_noquery.json"));
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatistics(StatsRecordType.SEARCH, "uuid", searchResponseMock,
					DateTools.stringToTime("20121221121212121"), qs);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			answ.assertError();
			Mockito.verifyNoMoreInteractions(tested.client);
		}

		// case - statistics enabled, search settings defined
		Mockito.reset(tested.client);
		qs.setQuery("my query");
		qs.setQueryHighlight(true);
		qs.addFacet(FacetValue.ACTIVITY_DATES_HISTOGRAM);
		qs.addFacet(FacetValue.TAG_CLOUD);
		qs.addField("_id");
		qs.addField("_source");
		qs.setSortBy(SortByValue.NEW);
		Filters filters = new Filters();
		qs.setFilters(filters);
		filters.addContributor("John Doe <john@doe.org>");
		filters.addContributor("Bill Hur <bill@hur.eu>");
		filters.addDcpType("issue");
		filters.addDcpType("blogpost");
		filters.addProject("jbossas7");
		filters.addProject("jbpm");
		filters.addTag("tag1");
		filters.addTag("tag2");
		filters.setActivityDateFrom(654653265465l);
		filters.setActivityDateTo(654653365465l);
		filters.setActivityDateInterval(PastIntervalValue.QUARTER);
		filters.setContentType("jbossorg_jira_issue");
		filters.setDcpContentProvider("jbossorg");
		filters.setFrom(128);
		filters.setSize(12);
		{
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_ok_query.json"));
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatistics(StatsRecordType.SEARCH, "uuid", searchResponseMock,
					DateTools.stringToTime("20121221121212121"), qs);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			answ.assertError();
			Mockito.verifyNoMoreInteractions(tested.client);
		}

		// case - exception from elasticsearch call is not thrown out of write method
		Mockito.reset(tested.client);
		tested.statsConfiguration = new StatsConfiguration(true);
		Mockito.doThrow(new RuntimeException("testException")).when(tested.client)
				.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

		tested.writeStatistics(StatsRecordType.SEARCH, "uuid", searchResponseMock,
				DateTools.stringToTime("20121221121212121"), qs);

		Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
		Mockito.verifyNoMoreInteractions(tested.client);
	}

	@SuppressWarnings("rawtypes")
	private class TestIndexingAnswer implements Answer {

		StatsClientService tested;
		String expectedSource;

		AssertionError err;

		private TestIndexingAnswer(StatsClientService tested, String expectedSource) {
			super();
			this.tested = tested;
			this.expectedSource = expectedSource;
		}

		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {
			try {
				Assert.assertEquals(tested.statsLogListener, invocation.getArguments()[1]);
				IndexRequest ir = (IndexRequest) invocation.getArguments()[0];
				Assert.assertEquals(StatsClientService.INDEX_NAME, ir.index());
				Assert.assertEquals(StatsClientService.INDEX_TYPE, ir.type());
				Assert.assertEquals(256, ir.timeout().getSeconds());
				TestUtils.assertJsonContent(expectedSource, ir.source().toUtf8());
			} catch (AssertionError e) {
				err = e;
			}
			return null;
		}

		public void assertError() {
			if (err != null)
				throw err;
		}

	}

}
