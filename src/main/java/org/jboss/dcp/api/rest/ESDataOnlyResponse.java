/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.IOException;
import java.io.OutputStream;

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
 *       JSON content of entity
 *     }
 *   ]
 * }
 * </pre>
 * 
 * @author Libor Krzyzanek
 * 
 */
public class ESDataOnlyResponse implements StreamingOutput {

	private SearchResponse response;

	public ESDataOnlyResponse(SearchResponse response) {
		this.response = response;
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		XContentBuilder builder = XContentFactory.jsonBuilder(output);
		// shows only hits
		builder.startObject();
		builder.field("total", response.getHits().getTotalHits());
		builder.startArray("hits");
		SearchHit[] hits = response.getHits().getHits();
		for (int i = 0; i < hits.length; i++) {
			builder.startObject();
			builder.field("id", hits[i].getId());
			builder.field("data", hits[i].sourceAsMap());
			builder.endObject();
		}
		builder.endArray();
		builder.endObject();
		builder.close();
	}
}