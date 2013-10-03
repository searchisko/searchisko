/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.Date;
import java.util.LinkedHashMap;
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
import javax.ws.rs.core.Response.Status;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.sort.SortOrder;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.annotations.header.CORSSupport;
import org.searchisko.api.annotations.security.GuestAllowed;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.persistence.service.ContentPersistenceService;

/**
 * REST API for Content
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
@RequestScoped
@Path("/content/{type}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ProviderAllowed
public class ContentRestService extends RestServiceBase {

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected ContentPersistenceService contentPersistenceService;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	@CORSSupport
	public Object getAllContent(@PathParam("type") String type, @QueryParam("from") Integer from,
			@QueryParam("size") Integer size, @QueryParam("sort") String sort) {
		if (type == null || type.isEmpty()) {
//			return createRequiredFieldResponse("type");
		}
		try {
			Map<String, Object> typeDef = providerService.findContentType(type);
			if (typeDef == null) {
                throw new BadFieldException("type");
			}

			String indexName = ProviderService.extractIndexName(typeDef, type);
			String indexType = ProviderService.extractIndexType(typeDef, type);

			SearchRequestBuilder srb = new SearchRequestBuilder(searchClientService.getClient());
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
					srb.addSort(ContentObjectFields.SYS_UPDATED, SortOrder.ASC);
				} else if (sort.equalsIgnoreCase(SortOrder.DESC.name())) {
					srb.addSort(ContentObjectFields.SYS_UPDATED, SortOrder.DESC);
				}
			}

			final SearchResponse response = srb.execute().actionGet();

			return new ESDataOnlyResponse(response, ContentObjectFields.SYS_CONTENT_ID);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
//		} catch (Exception e) {
//			return createErrorResponse(e);
		}
	}

	@GET
	@Path("/{contentId}")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	@CORSSupport
	public Object getContent(@PathParam("type") String type, @PathParam("contentId") String contentId) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
			throw new RequiredFieldException("contentId");
		}
		if (type == null || type.isEmpty()) {
            throw new RequiredFieldException("type");
		}
		try {
			Map<String, Object> typeDef = providerService.findContentType(type);
			if (typeDef == null) {
                throw new BadFieldException("type");
			}

			String sysContentId = providerService.generateSysId(type, contentId);

			String indexName = ProviderService.extractIndexName(typeDef, type);
			String indexType = ProviderService.extractIndexType(typeDef, type);

			GetResponse getResponse = searchClientService.getClient().prepareGet(indexName, indexType, sysContentId)
					.execute().actionGet();

			if (!getResponse.isExists()) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			return createResponse(getResponse);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
//		} catch (Exception e) {
//			return createErrorResponse(e);
		}
	}

	@POST
	@Path("/{contentId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@ProviderAllowed
	public Object pushContent(@PathParam("type") String type, @PathParam("contentId") String contentId,
			Map<String, Object> content) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
            throw new RequiredFieldException("contentId");
		}
		if (type == null || type.isEmpty()) {
            throw new RequiredFieldException("type");
		}
		if (content == null || content.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("Some content for pushing must be defined").build();
		}
//		try {
			Map<String, Object> provider = providerService.findProvider(getProvider());
			Map<String, Object> typeDef = ProviderService.extractContentType(provider, type);
			if (typeDef == null) {
                throw new BadFieldException("type");
			}

			String sysContentId = providerService.generateSysId(type, contentId);

			// check search subsystem configuration
			String indexName = ProviderService.extractIndexName(typeDef, type);
			String indexType = ProviderService.extractIndexType(typeDef, type);

			// fill some normalized fields - should be last step to avoid changing them via preprocessors
			content.put(ContentObjectFields.SYS_CONTENT_PROVIDER, getProvider());
			content.put(ContentObjectFields.SYS_CONTENT_ID, contentId);
			content.put(ContentObjectFields.SYS_CONTENT_TYPE, type);
			content.put(ContentObjectFields.SYS_ID, sysContentId);
			content.put(ContentObjectFields.SYS_TYPE, ProviderService.extractSysType(typeDef, type));
			content.put(ContentObjectFields.SYS_UPDATED, new Date());
			// Copy distinct data from content to normalized fields
			content.put(ContentObjectFields.SYS_TAGS, content.get("tags"));

			// Fill type of content from configuration
			if (content.containsKey(ContentObjectFields.SYS_CONTENT)) {
				content.put(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE,
						ProviderService.extractSysContentContentType(typeDef, type));
			} else {
				content.remove(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE);
			}

			// Run preprocessors to manipulate other fields
			providerService.runPreprocessors(type, ProviderService.extractPreprocessors(typeDef, type), content);

			// Refill type of content from configuration if content was added in preprocessors
			if (content.containsKey(ContentObjectFields.SYS_CONTENT)
					&& !content.containsKey(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE)) {
				content.put(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE,
						ProviderService.extractSysContentContentType(typeDef, type));
			}

			if (ProviderService.extractPersist(typeDef)) {
				contentPersistenceService.store(sysContentId, type, content);
			}

			// TODO EXTERNAL_TAGS - add external tags for this document into sys_tags field

			// Push to search subsystem
			IndexResponse ir = searchClientService.getClient().prepareIndex(indexName, indexType, sysContentId)
					.setSource(content).execute().actionGet();
			Map<String, String> retJson = new LinkedHashMap<String, String>();
			if (ir.getVersion() > 1) {
				retJson.put("status", "update");
				retJson.put("message", "Content was updated successfully.");
			} else {
				retJson.put("status", "insert");
				retJson.put("message", "Content was inserted successfully.");
			}
			return Response.ok(retJson).build();
//		} catch (Exception e) {
//			return createErrorResponse(e);
//		}
	}

	@DELETE
	@Path("/{contentId}")
	@ProviderAllowed
	public Object deleteContent(@PathParam("type") String type, @PathParam("contentId") String contentId,
			@QueryParam("ignore_missing") String ignoreMissing) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
			throw new RequiredFieldException("contentId");
		}
		if (type == null || type.isEmpty()) {
			throw new RequiredFieldException("type");
		}
//		try {
			Map<String, Object> provider = providerService.findProvider(getProvider());
			Map<String, Object> typeDef = ProviderService.extractContentType(provider, type);
			if (typeDef == null) {
				throw new BadFieldException("type");
			}

			String sysContentId = providerService.generateSysId(type, contentId);

			if (ProviderService.extractPersist(typeDef)) {
				contentPersistenceService.delete(sysContentId, type);
			}

			String indexName = ProviderService.extractIndexName(typeDef, type);
			String indexType = ProviderService.extractIndexType(typeDef, type);

			DeleteResponse dr = searchClientService.getClient().prepareDelete(indexName, indexType, sysContentId).execute()
					.actionGet();

			if (dr.isNotFound() && !Boolean.parseBoolean(ignoreMissing)) {
				return Response.status(Status.NOT_FOUND).entity("Content not found to be deleted.").build();
			} else {
				return Response.ok("Content deleted successfully.").build();
			}
//		} catch (Exception e) {
//			return createErrorResponse(e);
//		}
	}
}
