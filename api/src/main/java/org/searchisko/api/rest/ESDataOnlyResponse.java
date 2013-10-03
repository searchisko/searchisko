/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.search.SearchHit;

/**
 * WS Response for Elasticsearch serving only data from elasticsearch.<br/>
 * Output is following:
 *
 * <pre>
 * {
 * "total" : 1,
 * "hits"  : [
 *     {
 *       "id" : "12",
 *       "data" : {
 *         JSON content of entity - source field from ES index
 *       }
 *     }
 *   ]
 * }
 * </pre>
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
public class ESDataOnlyResponse implements StreamingOutput {

	/**
	 * Elastic search response
	 */
	private SearchResponse response;

	/**
	 * Name of field in document source which is returned as id in results. If null then ElasticSearch document id is
	 * returned as id.
	 */
	private String idField;

	/**
	 * Field names that should be removed from the output
	 */
	private String[] fieldsToRemove;

	/**
	 * Create new response object.
	 *
	 * @param response
	 */
	public ESDataOnlyResponse(SearchResponse response) {
		this.response = response;
	}

	/**
	 * Create new response object.
	 *
	 * @param response
	 * @param idField - name of field in document source which is returned as id in results.
	 */
	public ESDataOnlyResponse(SearchResponse response, String idField) {
		super();
		this.response = response;
		this.idField = idField;
	}

	/**
	 * Create new response
	 *
	 * @param response
	 * @param fieldsToRemove - field names from document source that should be removed
	 */
	public ESDataOnlyResponse(SearchResponse response, String[] fieldsToRemove) {
		this.response = response;
		this.fieldsToRemove = fieldsToRemove;
	}

	/**
	 * Create new response
	 *
	 * @param response
	 * @param idField - name of field in document source which is returned as id in results.
	 * @param fieldsToRemove - field names from document source that should be removed
	 */
	public ESDataOnlyResponse(SearchResponse response, String idField, String[] fieldsToRemove) {
		super();
		this.response = response;
		this.idField = idField;
		this.fieldsToRemove = fieldsToRemove;
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		XContentBuilder builder = XContentFactory.jsonBuilder(output);
		// shows only hits
		builder.startObject();
		if (response != null) {
			builder.field("total", response.getHits().getTotalHits());
			builder.startArray("hits");
			SearchHit[] hits = response.getHits().getHits();
			for (int i = 0; i < hits.length; i++) {
				builder.startObject();
				Map<String, Object> src = hits[i].sourceAsMap();
				if (idField == null) {
					builder.field("id", hits[i].getId());
				} else {
					builder.field("id", src.get(idField));
				}
				builder.field("data", ESDataOnlyResponse.removeFields(src, fieldsToRemove));
				builder.endObject();
			}
		} else {
			builder.field("total", 0);
			builder.startArray("hits");
		}
		builder.endArray();
		builder.endObject();
		builder.close();
	}

	/**
	 * Remove named fields from data map.
	 *
	 * @param data to remove fields from (can be null)
	 * @param fieldsToRemove name of fields to remove (can be null)
	 * @return data
	 */
	public static Map<String, Object> removeFields(Map<String, Object> data, String[] fieldsToRemove) {
		if (data == null) {
			return null;
		}

		if (fieldsToRemove == null) {
			return data;
		}

		for (String fieldName : fieldsToRemove) {
			data.remove(fieldName);
		}

		return data;
	}
}