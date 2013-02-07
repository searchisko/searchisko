/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;
import org.jboss.dcp.api.config.StatsConfiguration;
import org.jboss.dcp.api.config.TimeoutConfiguration;
import org.jboss.dcp.api.model.AppConfiguration.ClientType;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.util.SearchUtils;

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

	protected static final String INDEX_NAME = "stats";
	protected static final String INDEX_TYPE = "stats";

	@Inject
	protected StatsConfiguration statsConfiguration;

	@Inject
	protected TimeoutConfiguration timeout;

	protected ActionListener<IndexResponse> statsLogListener;

	@PostConstruct
	public void init() throws Exception {
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
		Properties settings = SearchUtils.loadProperties("/stats_client_settings.properties");

		if (ClientType.EMBEDDED.equals(appConfigurationService.getAppConfiguration().getClientType())) {
			node = createEmbeddedNode("stats", settings);
			client = node.client();
		} else {
			Properties transportAddresses = SearchUtils.loadProperties("/stats_client_connections.properties");
			client = createTransportClient(transportAddresses, settings);
		}

		checkHealthOfCluster(client);
	}

	/**
	 * Write ES search statistics record about unsuccessful search.
	 * 
	 * @param type of search performed - mandatory
	 * @param ex exception from search attempt - mandatory
	 * @param dateInMillis timestamp when search was performed
	 * @param query search query performed
	 * @param filters used for search - optional
	 */
	public void writeStatistics(StatsRecordType type, ElasticSearchException ex, long dateInMillis,
			QuerySettings querySettings) {

		if (!statsConfiguration.enabled()) {
			return;
		}

		Map<String, Object> source = new HashMap<String, Object>();

		source.put("type", type.name().toLowerCase());
		source.put("date", dateInMillis);
		source.put("exception", true);
		source.put("exception_detailed_message", ex.getDetailedMessage());
		source.put("exception_most_specific_cause", ex.getMostSpecificCause());
		source.put("status", ex.status());

		addQuery(source, querySettings);

		try {
			IndexRequest ir = Requests.indexRequest().index(INDEX_NAME).type(INDEX_TYPE)
					.timeout(TimeValue.timeValueSeconds(timeout.stats())).source(source);
			// async call, if it fails -> just log
			client.index(ir, statsLogListener);
		} catch (Throwable e) {
			log.log(Level.FINEST, "Error writing into stats server: " + e.getMessage(), e);
		}
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
	public void writeStatistics(StatsRecordType type, String responseUuid, SearchResponse resp, long dateInMillis,
			QuerySettings querySettings) {

		if (!statsConfiguration.enabled()) {
			return;
		}

		if (resp == null) {
			return;
		}

		Map<String, Object> source = new HashMap<String, Object>();

		source.put("type", type.name().toLowerCase());
		source.put("date", DATE_TIME_FORMATTER_UTC.print(dateInMillis));
		source.put("response_uuid", responseUuid);
		source.put("took", resp.tookInMillis());
		source.put("timed_out", resp.timedOut());
		source.put("total_hits", resp.hits().totalHits());
		source.put("max_score", resp.hits().maxScore());
		source.put("shards_successful", resp.successfulShards());
		source.put("shards_failed", resp.failedShards());
		source.put("status", resp.status().name());
		if (resp.failedShards() > 0) {
			for (ShardSearchFailure ssf : resp.getShardFailures()) {
				source.put("shard_failure", ssf.reason());
			}
		}

		addQuery(source, querySettings);

		if (resp.hits().totalHits() > 0) {
			List<String> hitIds = new ArrayList<String>();
			for (SearchHit hit : resp.hits().hits()) {
				hitIds.add(hit.getId());
			}
			source.put("returned_hits", hitIds.size());
			source.put("hits_id", hitIds);

		}

		try {
			IndexRequest ir = Requests.indexRequest().index(INDEX_NAME).type(INDEX_TYPE)
					.timeout(TimeValue.timeValueSeconds(timeout.stats())).source(source);
			// async call, if it fails -> just log
			client.index(ir, statsLogListener);
		} catch (Throwable e) {
			log.log(Level.FINEST, "Error writing into stats server: " + e.getMessage(), e);
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
			if (filters.getDcpTypes() != null && !filters.getDcpTypes().isEmpty()) {
				source.put("filters_dcp_types", filters.getDcpTypes());
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
			if (filters.getDcpContentProvider() != null) {
				source.put("filters_dcp_content_provider", filters.getDcpContentProvider());
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
