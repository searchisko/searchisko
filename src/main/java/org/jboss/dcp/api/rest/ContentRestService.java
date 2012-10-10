/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.Date;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.annotations.security.ProviderAllowed;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.StatsRecordType;

/**
 * REST API for Content
 * 
 * @author Libor Krzyzanek
 * 
 */
@RequestScoped
@Path("/content/{type}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ProviderAllowed
public class ContentRestService extends RestServiceBase {

	@Inject
	private ProviderService providerService;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	public Object getAllContent(@PathParam("type") String type, @QueryParam("from") Integer from,
			@QueryParam("size") Integer size, @QueryParam("sort") String sort) {
		if (type == null) {
			return createRequiredFieldResponse("type");
		}
		try {
			Map<String, Object> typeDef = providerService.findContentType(type);
			if (typeDef == null) {
				return createBadFieldDataResponse("type");
			}

			String indexName = ProviderService.getIndexName(typeDef);
			String indexType = ProviderService.getIndexType(typeDef);

			SearchRequestBuilder srb = new SearchRequestBuilder(getSearchClientService().getClient());
			srb.setIndices(indexName);
			srb.setTypes(indexType);

			srb.setQuery(QueryBuilders.matchAllQuery());

			if (from != null) {
				srb.setFrom(from);
			}
			if (size != null) {
				srb.setSize(size);
			}
			if (sort != null) {
				if (sort.equalsIgnoreCase(SortOrder.ASC.name())) {
					srb.addSort("dcp_updated", SortOrder.ASC);
				} else if (sort.equalsIgnoreCase(SortOrder.DESC.name())) {
					srb.addSort("dcp_updated", SortOrder.DESC);
				}
			}

			final SearchResponse response = srb.execute().actionGet();

			return new ESDataOnlyResponse(response);
		} catch (ElasticSearchException e) {
			return createErrorResponse(e);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	@GET
	@Path("/{contentId}")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	public Object getContent(@PathParam("type") String type, @PathParam("contentId") String contentId) {

		// validation
		if (contentId == null) {
			return createRequiredFieldResponse("contentId");
		}
		if (type == null) {
			return createRequiredFieldResponse("type");
		}
		try {
			Map<String, Object> typeDef = providerService.findContentType(type);
			if (typeDef == null) {
				return createBadFieldDataResponse("type");
			}

			String indexName = ProviderService.getIndexName(typeDef);
			String indexType = ProviderService.getIndexType(typeDef);

			GetResponse getResponse = getSearchClientService().getClient().prepareGet(indexName, indexType, contentId)
					.execute().actionGet();

			return createResponse(getResponse);
		} catch (ElasticSearchException e) {
			getStatsClientService().writeStatistics(StatsRecordType.DOCUMENT_DETAIL, e, System.currentTimeMillis(),
					contentId, null);
			return createErrorResponse(e);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	@POST
	@Path("/{contentId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Object pushContent(@PathParam("type") String type, @PathParam("contentId") String contentId,
			Map<String, Object> content) {

		// validation
		if (contentId == null) {
			return createRequiredFieldResponse("contentId");
		}
		if (type == null) {
			return createRequiredFieldResponse("type");
		}
		Map<String, Object> provider = providerService.findProvider(getProvider());
		Map<String, Object> typeDef = ProviderService.getContentType(provider, type);
		if (typeDef == null) {
			return createBadFieldDataResponse("type");
		}

		// normalize content
		content.put("dcp_id", providerService.generateDcpId(type, contentId));
		content.put("dcp_type", ProviderService.getDcpType(typeDef));
		content.put("dcp_updated", new Date());

		// Run preprocessors
		providerService.runPreprocessors(ProviderService.getPreprocessors(typeDef), content);

		// Push to search
		String index = ProviderService.getIndexName(typeDef);
		String indexType = ProviderService.getIndexType(typeDef);

		// TODO: Store to Persistance

		try {
			getSearchClientService().getClient().prepareIndex(index, indexType, contentId).setSource(content).execute()
					.actionGet();
		} catch (Exception e) {
			return createErrorResponse(e);
		}

		return Response.ok("Content was successfully pushed").build();
	}

	@DELETE
	@Path("/{contentId}")
	@ProviderAllowed
	public Object deleteContent(@PathParam("type") String type, @PathParam("contentId") String contentId) {
		if (type == null) {
			return createRequiredFieldResponse("type");
		}
		Map<String, Object> provider = providerService.findProvider(getProvider());
		Map<String, Object> typeDef = ProviderService.getContentType(provider, type);
		if (typeDef == null) {
			return createBadFieldDataResponse("type");
		}

		// TODO: Remove from persistance if exists

		String index = ProviderService.getIndexName(typeDef);
		String indexType = ProviderService.getIndexType(typeDef);
		getSearchClientService().getClient().prepareDelete(index, indexType, contentId).execute().actionGet();

		return Response.ok().build();
	}
}
