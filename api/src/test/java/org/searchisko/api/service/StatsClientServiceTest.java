/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.lucene.document.DateTools;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.text.StringText;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.searchisko.api.model.*;
import org.searchisko.api.model.AppConfiguration.ClientType;
import org.searchisko.api.model.QuerySettings.Filters;
import org.searchisko.api.testtools.TestUtils;

/**
 * Unit test for {@link StatsClientService}
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StatsClientServiceTest {

	// TODO: should be on one place only
	private static final DateTimeFormatter DATE_TIME_FORMATTER_UTC = ISODateTimeFormat.dateTime().withZoneUTC();

	@Test
	public void init_embedded() throws Exception {
		AppConfigurationService acs = new AppConfigurationService();
		AppConfiguration ac = new AppConfiguration(System.getProperty("java.io.tmpdir"));
		acs.appConfiguration = ac;
		ac.setClientType(ClientType.EMBEDDED);
		ac.setProviderCreateInitData(false);

		StatsClientService tested = new StatsClientService();
		tested.appConfigurationService = acs;
		tested.log = Logger.getLogger("testlogger");

		tested.statsConfiguration = new StatsConfiguration(true, false, true);

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
	public void init_useSearchCluster() throws Exception {
		StatsClientService tested = new StatsClientService();
		tested.log = Logger.getLogger("testlogger");
		tested.statsConfiguration = new StatsConfiguration(true, true, true);
		Client mockClient = Mockito.mock(Client.class);
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		Mockito.when(tested.searchClientService.getClient()).thenReturn(mockClient);
		try {
			tested.init();
			Assert.assertNull(tested.node);
			Assert.assertNotNull(tested.client);
			Assert.assertEquals(mockClient, tested.client);
			Assert.assertNotNull(tested.statsLogListener);
		} finally {
			tested.destroy();
			Assert.assertNull(tested.node);
			Assert.assertNull(tested.client);
			Mockito.verifyZeroInteractions(mockClient);
		}
	}

	@Test
	public void init_transport() {
		// untestable :-(
	}

	@Test
	public void init_disabled() throws Exception {
		// no initialization is performed if statistics are disabled
		StatsClientService tested = new StatsClientService();
		tested.log = Logger.getLogger("testlogger");
		tested.statsConfiguration = new StatsConfiguration(false, true, true);
		Client mockClient = Mockito.mock(Client.class);
		tested.searchClientService = Mockito.mock(SearchClientService.class);
		Mockito.when(tested.searchClientService.getClient()).thenReturn(mockClient);
		try {
			tested.init();
			Assert.assertNull(tested.node);
			Assert.assertNull(tested.client);
			Assert.assertNotNull(tested.statsLogListener);
		} finally {
			tested.destroy();
			Assert.assertNull(tested.node);
			Assert.assertNull(tested.client);
			Mockito.verifyZeroInteractions(mockClient);
			Mockito.verifyZeroInteractions(tested.searchClientService);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeStatisticsRecord_Exception() throws ParseException, IOException {
		final StatsClientService tested = getTested();

		// case - statistics disabled
		QuerySettings qs = new QuerySettings();
		qs.setQuery("my query");
		tested.statsConfiguration = new StatsConfiguration(false);
		tested.writeStatisticsRecord(StatsRecordType.SEARCH, new ElasticsearchException("Test exception"),
				DateTools.stringToTime("20121221121212121"), qs);
		Mockito.verifyZeroInteractions(tested.client);

		// case - statistics enabled, no filter defined
		qs.setFilters(new Filters());
		Mockito.reset(tested.client);
		tested.statsConfiguration = new StatsConfiguration(true);
		{
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_exception_noquery.json"), StatsRecordType.SEARCH);
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatisticsRecord(StatsRecordType.SEARCH, new ElasticsearchException("Test exception"),
					DateTools.stringToTime("20121221121212121"), qs);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			answ.assertError();
			Mockito.verifyNoMoreInteractions(tested.client);
		}

		// case - statistics enabled, search settings defined
		Mockito.reset(tested.client);
		qs.setQuery("my query");
		qs.setQueryHighlight(true);
		qs.addAggregation("activity_dates_histogram");
		qs.addAggregation("tag_cloud");
		qs.addField("_id");
		qs.addField("_source");
		qs.setSortBy(SortByValue.NEW);
		qs.setSize(12);
		qs.setFrom(128);
		Filters filters = new Filters();
		qs.setFilters(filters);
		filters.acknowledgeUrlFilterCandidate("contributor", "John Doe <john@doe.org>", "Bill Hur <bill@hur.eu>");
		filters.acknowledgeUrlFilterCandidate("sys_type", "issue", "blogpost");
		filters.acknowledgeUrlFilterCandidate("project", "jbossas7", "jbpm");
		filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
		filters.acknowledgeUrlFilterCandidate("activity_date_from", new DateTime(654653265465l).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(654653365465l).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.QUARTER.toString());
		filters.acknowledgeUrlFilterCandidate("type", "jbossorg_jira_issue");
		filters.acknowledgeUrlFilterCandidate("content_provider", "jbossorg");
		{
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_exception_query.json"), StatsRecordType.SEARCH);
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatisticsRecord(StatsRecordType.SEARCH, new ElasticsearchException("Test exception"),
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

		tested.writeStatisticsRecord(StatsRecordType.SEARCH, new ElasticsearchException("Test exception"),
				DateTools.stringToTime("20121221121212121"), qs);

		Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
		Mockito.verifyNoMoreInteractions(tested.client);
	}

	@SuppressWarnings("unchecked")
	private StatsClientService getTested() {
		final StatsClientService tested = new StatsClientService();
		tested.client = Mockito.mock(Client.class);
		tested.log = Logger.getLogger("testlogger");
		tested.statsLogListener = Mockito.mock(ActionListener.class);
		tested.timeout = Mockito.mock(TimeoutConfiguration.class);
		Mockito.when(tested.timeout.stats()).thenReturn(256);
		return tested;
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeStatisticsRecord_OK() throws ParseException, IOException {
		final StatsClientService tested = getTested();

		SearchResponse searchResponseMock = Mockito.mock(SearchResponse.class);

		// case - statistics disabled
		QuerySettings qs = new QuerySettings();

		tested.statsConfiguration = new StatsConfiguration(false);
		tested.writeStatisticsRecord(StatsRecordType.SEARCH, "uuid", searchResponseMock,
				DateTools.stringToTime("20121221121212121"), qs);
		Mockito.verifyZeroInteractions(tested.client);

		// case - search response null
		tested.statsConfiguration = new StatsConfiguration(true);
		tested.writeStatisticsRecord(StatsRecordType.SEARCH, "uuid", (SearchResponse) null,
				DateTools.stringToTime("20121221121212121"), qs);
		Mockito.verifyZeroInteractions(tested.client);

		// case - statistics enabled, empty search settings
		Mockito.reset(tested.client);
		tested.statsConfiguration = new StatsConfiguration(true);
		SearchHits hitsMock = Mockito.mock(SearchHits.class);
		SearchHit[] hitsValues = new SearchHit[5];
		for (int i = 0; i < hitsValues.length; i++) {
			hitsValues[i] = new InternalSearchHit(i, "idx" + i, new StringText("type"), null);
		}
		Mockito.when(hitsMock.totalHits()).thenReturn(129l);
		Mockito.when(hitsMock.maxScore()).thenReturn(29.3f);
		Mockito.when(hitsMock.getHits()).thenReturn(hitsValues);
		Mockito.when(searchResponseMock.getHits()).thenReturn(hitsMock);
		Mockito.when(searchResponseMock.status()).thenReturn(RestStatus.OK);
		Mockito.when(searchResponseMock.getTookInMillis()).thenReturn(456l);
		Mockito.when(searchResponseMock.getSuccessfulShards()).thenReturn(2);
		Mockito.when(searchResponseMock.getFailedShards()).thenReturn(1);
		Mockito.when(searchResponseMock.getShardFailures()).thenReturn(
				new ShardSearchFailure[] { new ShardSearchFailure("test reason", null) });

		{
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_ok_noquery.json"), StatsRecordType.SEARCH);
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatisticsRecord(StatsRecordType.SEARCH, "uuid", searchResponseMock,
					DateTools.stringToTime("20121221121212121"), qs);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			answ.assertError();
			Mockito.verifyNoMoreInteractions(tested.client);
		}

		// case - statistics enabled, search settings defined
		Mockito.reset(tested.client);
		qs.setQuery("my query");
		qs.setQueryHighlight(true);
		qs.addAggregation("activity_dates_histogram");
		qs.addAggregation("tag_cloud");
		qs.addField("_id");
		qs.addField("_source");
		qs.setSortBy(SortByValue.NEW);
		qs.setFrom(12);
		qs.setSize(128);
		Filters filters = new Filters();
		qs.setFilters(filters);
		filters.acknowledgeUrlFilterCandidate("contributor", "John Doe <john@doe.org>", "Bill Hur <bill@hur.eu>");
		filters.acknowledgeUrlFilterCandidate("sys_type", "issue", "blogpost");
		filters.acknowledgeUrlFilterCandidate("project", "jbossas7", "jbpm");
		filters.acknowledgeUrlFilterCandidate("tag", "tag1", "tag2");
		filters.acknowledgeUrlFilterCandidate("activity_date_from", new DateTime(654653265465l).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_to", new DateTime(654653365465l).toString(DATE_TIME_FORMATTER_UTC));
		filters.acknowledgeUrlFilterCandidate("activity_date_interval", PastIntervalValue.QUARTER.toString());
		filters.acknowledgeUrlFilterCandidate("type", "jbossorg_jira_issue");
		filters.acknowledgeUrlFilterCandidate("content_provider", "jbossorg");
		{
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_ok_query.json"), StatsRecordType.SEARCH);
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatisticsRecord(StatsRecordType.SEARCH, "uuid", searchResponseMock,
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

		tested.writeStatisticsRecord(StatsRecordType.SEARCH, "uuid", searchResponseMock,
				DateTools.stringToTime("20121221121212121"), qs);

		Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
		Mockito.verifyNoMoreInteractions(tested.client);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeStatisticsRecord_SOURCE() throws ParseException, IOException {
		final StatsClientService tested = getTested();

		// case - statistics disabled
		tested.statsConfiguration = new StatsConfiguration(false);
		tested.writeStatisticsRecord(StatsRecordType.SEARCH, 125456, null);
		Mockito.verifyZeroInteractions(tested.client);

		// case - stats enabled and source is null
		tested.statsConfiguration = new StatsConfiguration(true);
		{
			Mockito.reset(tested.client);
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_source_1.json"), StatsRecordType.SEARCH_HIT_USED);
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatisticsRecord(StatsRecordType.SEARCH_HIT_USED, DateTools.stringToTime("20121221121212121"), null);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			answ.assertError();
			Mockito.verifyNoMoreInteractions(tested.client);
		}

		// case - stats enabled and source passed in
		tested.statsConfiguration = new StatsConfiguration(true);
		{
			Mockito.reset(tested.client);
			TestIndexingAnswer answ = new TestIndexingAnswer(tested,
					TestUtils.readStringFromClasspathFile("/stats/stat_source_2.json"), StatsRecordType.SEARCH_HIT_USED);
			Mockito.doAnswer(answ).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			Map<String, Object> source = new HashMap<String, Object>();
			source.put("field1", "value1");
			source.put("field2", 1023);
			tested
					.writeStatisticsRecord(StatsRecordType.SEARCH_HIT_USED, DateTools.stringToTime("20121221121212121"), source);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			answ.assertError();
			Mockito.verifyNoMoreInteractions(tested.client);
		}

		// case - exception from elasticsearch call is not thrown out of write method
		{
			Mockito.reset(tested.client);
			tested.statsConfiguration = new StatsConfiguration(true);
			Mockito.doThrow(new RuntimeException("testException")).when(tested.client)
					.index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));

			tested.writeStatisticsRecord(StatsRecordType.SEARCH_HIT_USED, DateTools.stringToTime("20121221121212121"), null);

			Mockito.verify(tested.client).index(Mockito.any(IndexRequest.class), Mockito.any(ActionListener.class));
			Mockito.verifyNoMoreInteractions(tested.client);
		}
	}

	@Test
	public void checkStatisticsRecordExists() {
		// TODO SEARCH STATS UNITTEST
		// test case when stats are disabled!
	}

	@SuppressWarnings("rawtypes")
	private class TestIndexingAnswer implements Answer {

		StatsClientService tested;
		String expectedSource;
		StatsRecordType expectedRecordType;

		AssertionError err;

		private TestIndexingAnswer(StatsClientService tested, String expectedSource, StatsRecordType expectedRecordType) {
			super();
			this.tested = tested;
			this.expectedSource = expectedSource;
			this.expectedRecordType = expectedRecordType;
		}

		@Override
		public Object answer(InvocationOnMock invocation) throws Throwable {
			try {
				Assert.assertEquals(tested.statsLogListener, invocation.getArguments()[1]);
				IndexRequest ir = (IndexRequest) invocation.getArguments()[0];
				Assert.assertEquals(expectedRecordType.getSearchIndexName(), ir.index());
				Assert.assertEquals(expectedRecordType.getSearchIndexType(), ir.type());
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
