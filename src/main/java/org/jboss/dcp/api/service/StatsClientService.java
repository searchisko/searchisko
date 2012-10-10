/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.elasticsearch.common.unit.TimeValue;
import org.jboss.dcp.api.config.StatsConfiguation;
import org.jboss.dcp.api.config.TimeoutConfiguration;
import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.AppConfiguration.ClientType;
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

	@Inject
	private Logger log;

	@Inject
	private StatsConfiguation statsConfiguration;

	@Inject
	private TimeoutConfiguration timeout;

	private ActionListener<IndexResponse> statsLogListener;

	@PostConstruct
	public void init() throws Exception {
		statsLogListener = new ActionListener<IndexResponse>() {
			@Override
			public void onResponse(IndexResponse indexResponse) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "stats: {0}", indexResponse);
				}
			}

			@Override
			public void onFailure(Throwable e) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "stats: ", e);
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

	public void writeStatistics(String type, ElasticSearchException ex, long dateInMillis, String query,
			QuerySettings.Filters filters) {

		if (!statsConfiguration.enabled()) {
			return;
		}

		Map<String, Object> source = new HashMap<String, Object>();

		source.put("type", type);
		source.put("date", dateInMillis);
		source.put("query_string", query);
		source.put("exception", true);
		source.put("exception_detailed_message", ex.getDetailedMessage());
		source.put("exception_most_specific_cause", ex.getMostSpecificCause());
		source.put("status", ex.status());

		if (filters != null) {
			if (filters.getAuthor() != null && filters.getAuthor().length > 0) {
				source.put("filters_authors", filters.getAuthor());
			}
			if (filters.getFrom() != null) {
				source.put("filters_from", filters.getFrom());
			}
			if (filters.getMailList() != null && filters.getMailList().length > 0) {
				source.put("filters_mailLists", filters.getMailList());
			}
			if (filters.getProject() != null && filters.getProject().length > 0) {
				source.put("filters_projects", filters.getProject());
			}
			if (filters.getStart() != null) {
				source.put("filters_start", filters.getStart());
			}
			if (filters.getTo() != null) {
				source.put("filters_to", filters.getTo());
			}
		}

		try {
			IndexRequest ir = Requests.indexRequest().index("stats").type("stats")
					.timeout(TimeValue.timeValueSeconds(timeout.stats())).source(source);
			// async call, if it fails -> just log
			client.index(ir, statsLogListener);
		} catch (Throwable e) {
			log.log(Level.FINEST, "Error writing into stats server", e);
		}
	}

	public void writeStatistics(String type, SearchResponse resp, long dateInMillis, String query,
			QuerySettings.Filters filters) {

		if (!statsConfiguration.enabled()) {
			return;
		}

		if (resp == null) {
			return;
		}

		Map<String, Object> source = new HashMap<String, Object>();

		source.put("type", type);
		source.put("date", dateInMillis);
		source.put("query_string", query);
		source.put("took", resp.tookInMillis());
		source.put("timed_out", resp.timedOut());
		source.put("total_hits", resp.hits().totalHits());
		source.put("max_score", resp.getHits().maxScore());
		source.put("shards_successful", resp.getSuccessfulShards());
		source.put("shards_failed", resp.failedShards());
		source.put("status", resp.status().name());
		if (resp.failedShards() > 0) {
			for (ShardSearchFailure ssf : resp.getShardFailures()) {
				source.put("shard_failure", ssf.reason());
			}
		}

		if (filters != null) {
			if (filters.getAuthor() != null && filters.getAuthor().length > 0) {
				source.put("filters_authors", filters.getAuthor());
			}
			if (filters.getFrom() != null) {
				source.put("filters_from", filters.getFrom());
			}
			if (filters.getMailList() != null && filters.getMailList().length > 0) {
				source.put("filters_mailLists", filters.getMailList());
			}
			if (filters.getProject() != null && filters.getProject().length > 0) {
				source.put("filters_projects", filters.getProject());
			}
			if (filters.getStart() != null) {
				source.put("filters_start", filters.getStart());
			}
			if (filters.getTo() != null) {
				source.put("filters_to", filters.getTo());
			}
		}

		try {
			IndexRequest ir = Requests.indexRequest().index("stats").type("stats")
					.timeout(TimeValue.timeValueSeconds(timeout.stats())).source(source);
			// async call, if it fails -> just log
			client.index(ir, statsLogListener);
		} catch (Throwable e) {
			log.log(Level.FINEST, "Error writing into stats server", e);
		}
	}

}
