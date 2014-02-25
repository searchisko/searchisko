/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.SearchHit;
import org.searchisko.api.model.AppConfiguration.ClientType;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.model.StatsConfiguration;
import org.searchisko.api.model.TimeoutConfiguration;
import org.searchisko.api.util.SearchUtils;

/**
 * Service for Elasticsearch StatsClient
 *
 * @author Libor Krzyzanek
 *
 */
@Named
@ApplicationScoped
@Singleton
@Startup
public class StatsClientService extends ElasticsearchClientService {

	public static final String FIELD_STATUS = "status";
	public static final String FIELD_DATE = "date";
	public static final String FIELD_TYPE = "type";
	public static final String FIELD_HITS_ID = "hits_id";
	public static final String FIELD_RESPONSE_UUID = "response_uuid";

	@Inject
	protected StatsConfiguration statsConfiguration;

	@Inject
	protected TimeoutConfiguration timeout;

	@Inject
	protected SearchClientService searchClientService;

	protected ActionListener<IndexResponse> statsLogListener;

	@PostConstruct
	public void init() throws Exception {
		log = Logger.getLogger(getClass().getName());
		statsLogListener = new ActionListener<IndexResponse>() {
			@Override
			public void onResponse(IndexResponse indexResponse) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "stats write response: {0}", indexResponse);
				}
			}

			@Override
			public void onFailure(Throwable e) {
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING, "stats write failed: " + e.getMessage(), e);
				}
			}
		};

		if (statsConfiguration.enabled()) {
			if (statsConfiguration.isUseSearchCluster()) {
				log.info("Statistics are enabled, search ES cluster is used");
				client = searchClientService.getClient();
			} else {
				Properties settings = SearchUtils.loadProperties("/stats_client_settings.properties");
				if (ClientType.EMBEDDED.equals(appConfigurationService.getAppConfiguration().getClientType())) {
					log.info("Statistics are enabled, embedded ES cluster is used");
					node = createEmbeddedNode("stats", settings);
					client = node.client();
				} else {
					log.info("Statistics are enabled, remote ES cluster is used");
					Properties transportAddresses = SearchUtils.loadProperties("/stats_client_connections.properties");
					client = createTransportClient(transportAddresses, settings);
				}
				checkHealthOfCluster(client);
			}
		} else {
			log.info("Statistics are disabled");
		}

	}

	@PreDestroy
	public void destroy() {
		if (node != null) {
			super.destroy();
		} else {
			client = null;
		}
	}

	/**
	 * Write ES search statistics record about unsuccessful search.
	 *
	 * @param type of search performed - mandatory
	 * @param ex exception from search attempt - mandatory
	 * @param dateInMillis timestamp when search was performed
	 * @param querySettings client query settings
	 */
	public void writeStatisticsRecord(StatsRecordType type, ElasticSearchException ex, long dateInMillis,
			QuerySettings querySettings) {

		if (!statsConfiguration.enabled()) {
			return;
		}

		Map<String, Object> source = new HashMap<>();

		source.put("exception", true);
		source.put("exception_detailed_message", ex.getDetailedMessage());
		source.put("exception_most_specific_cause", ex.getMostSpecificCause());
		source.put(FIELD_STATUS, ex.status());

		addQuery(source, querySettings);

		writeStatisticsRecord(type, dateInMillis, source);
	}

	/**
	 * Write ES search statistics record about successful search.
	 *
	 * @param type of search performed
	 * @param responseUuid UUID of response (also returned over search REST API)
	 * @param resp response from search attempt
	 * @param dateInMillis timestamp when search was performed
	 * @param querySettings performed
	 */
	public void writeStatisticsRecord(StatsRecordType type, String responseUuid, SearchResponse resp, long dateInMillis,
			QuerySettings querySettings) {

		if (!statsConfiguration.enabled()) {
			return;
		}

		if (resp == null) {
			return;
		}

		Map<String, Object> source = new HashMap<>();

		source.put(FIELD_RESPONSE_UUID, responseUuid);
		source.put("took", resp.getTookInMillis());
		source.put("timed_out", resp.isTimedOut());
		source.put("total_hits", resp.getHits().totalHits());
		source.put("max_score", resp.getHits().maxScore());
		source.put("shards_successful", resp.getSuccessfulShards());
		source.put("shards_failed", resp.getFailedShards());
		source.put(FIELD_STATUS, resp.status().name());
		if (resp.getFailedShards() > 0) {
			for (ShardSearchFailure ssf : resp.getShardFailures()) {
				source.put("shard_failure", ssf.reason());
			}
		}

		addQuery(source, querySettings);

		if (resp.getHits().totalHits() > 0) {
			List<String> hitIds = new ArrayList<>();
			for (SearchHit hit : resp.getHits().getHits()) {
				hitIds.add(hit.getId());
			}
			source.put("returned_hits", hitIds.size());
			source.put(FIELD_HITS_ID, hitIds);

		}

		writeStatisticsRecord(type, dateInMillis, source);
	}

	/**
	 * Write ES statistics record - general code.
	 *
	 * @param type of record
	 * @param dateInMillis timestamp when operation was performed
	 * @param source fields to be written into statistics record.
	 */
	public void writeStatisticsRecord(StatsRecordType type, long dateInMillis, Map<String, Object> source) {

		if (!statsConfiguration.enabled()) {
			return;
		}

		if (source == null)
			source = new HashMap<>();

		source.put(FIELD_TYPE, type.getSearchIndexedValue());
		source.put(FIELD_DATE, DATE_TIME_FORMATTER_UTC.print(dateInMillis));
		try {
			IndexRequest ir = Requests.indexRequest().index(type.getSearchIndexName()).type(type.getSearchIndexType())
					.timeout(TimeValue.timeValueSeconds(timeout.stats())).source(source);
			// async call, if it fails -> just log
			client.index(ir, statsLogListener);
		} catch (Throwable e) {
			log.log(Level.FINEST, "Error writing into stats server: " + e.getMessage(), e);
		}
	}

	/**
	 * Check if some statistics record exists for specified conditions.
	 *
	 * @param type of record we are looking for
	 * @param conditions for lookup. Key is a name of field to filter over, Value is a value to filter for using term
	 *          condition.
	 * @return true if at least one record matching conditions exits
	 */
	public boolean checkStatisticsRecordExists(StatsRecordType type, Map<String, Object> conditions) {
		if (!statsConfiguration.enabled()) {
			return false;
		}
		SearchRequestBuilder srb = new SearchRequestBuilder(client);
		srb.setIndices(type.getSearchIndexName());
		srb.setTypes(type.getSearchIndexType());
		AndFilterBuilder fb = new AndFilterBuilder();
		fb.add(new TermsFilterBuilder(FIELD_TYPE, type.getSearchIndexedValue()));
		if (conditions != null) {
			for (String fieldName : conditions.keySet()) {
				fb.add(new TermsFilterBuilder(fieldName, conditions.get(fieldName)));
			}
		}

		srb.setQuery(new FilteredQueryBuilder(QueryBuilders.matchAllQuery(), fb));
		srb.addField("_id");

		try {
			SearchResponse searchResponse = srb.execute().actionGet();

			return searchResponse.getHits().getTotalHits() > 0;
		} catch (org.elasticsearch.indices.IndexMissingException e) {
			return false;
		}
	}

	/**
	 * @param source
	 * @param querySettings
	 */
	protected void addQuery(Map<String, Object> source, QuerySettings querySettings) {
		if (querySettings != null) {
			if (querySettings.getQuery() != null) {
				source.put("query_string", querySettings.getQuery());
				source.put("query_highlight", querySettings.isQueryHighlight());
			}
			if (querySettings.getFields() != null && !querySettings.getFields().isEmpty())
				source.put("query_fields", querySettings.getFields());
			if (querySettings.getFacets() != null && !querySettings.getFacets().isEmpty())
				source.put("query_facets", querySettings.getFacets());
			if (querySettings.getSortBy() != null)
				source.put("query_sortBy", querySettings.getSortBy().toString());
			addFilters(source, querySettings.getFilters());
		}
	}

	private static final DateTimeFormatter DATE_TIME_FORMATTER_UTC = ISODateTimeFormat.dateTime().withZoneUTC();

	/**
	 * @param source
	 * @param filters
	 */
	protected void addFilters(Map<String, Object> source, QuerySettings.Filters filters) {
		if (filters != null) {
			if (filters.getContentType() != null) {
				source.put("filters_content_type", filters.getContentType());
			}
			if (filters.getProjects() != null && !filters.getProjects().isEmpty()) {
				source.put("filters_projects", filters.getProjects());
			}
			if (filters.getTags() != null && !filters.getTags().isEmpty()) {
				source.put("filters_tags", filters.getTags());
			}
			if (filters.getContributors() != null && !filters.getContributors().isEmpty()) {
				source.put("filters_contributors", filters.getContributors());
			}
			if (filters.getSysTypes() != null && !filters.getSysTypes().isEmpty()) {
				source.put("filters_sys_types", filters.getSysTypes());
			}
			if (filters.getActivityDateFrom() != null) {
				source.put("filters_activity_date_from", DATE_TIME_FORMATTER_UTC.print(filters.getActivityDateFrom()));
			}
			if (filters.getActivityDateTo() != null) {
				source.put("filters_activity_date_to", DATE_TIME_FORMATTER_UTC.print(filters.getActivityDateTo()));
			}
			if (filters.getActivityDateInterval() != null) {
				source.put("filters_activity_date_interval", filters.getActivityDateInterval().toString());
			}
			if (filters.getSysContentProvider() != null) {
				source.put("filters_sys_content_provider", filters.getSysContentProvider());
			}
			if (filters.getFrom() != null) {
				source.put("filters_from", filters.getFrom());
			}
			if (filters.getSize() != null) {
				source.put("filters_size", filters.getSize());
			}
		}
	}

}
