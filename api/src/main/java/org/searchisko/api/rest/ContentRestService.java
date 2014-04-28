/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.sort.SortOrder;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.annotations.security.GuestAllowed;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContentStoredEvent;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.rest.security.AuthenticationUtilService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.persistence.service.ContentPersistenceService;

/**
 * REST API for Content related operations.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
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

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Context
	protected SecurityContext securityContext;

	@Inject
	protected Event<ContentStoredEvent> eventContentStored;

	@Inject
	protected Event<ContentDeletedEvent> eventContentDeleted;

	@Inject
	protected Event<ContentBeforeIndexedEvent> eventBeforeIndexed;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	public Object getAllContent(@PathParam("type") String type, @QueryParam("from") Integer from,
			@QueryParam("size") Integer size, @QueryParam("sort") String sort) {
		if (type == null || SearchUtils.isBlank(type)) {
			throw new RequiredFieldException("type");
		}
		try {
			ProviderContentTypeInfo typeInfo = providerService.findContentType(type);
			if (typeInfo == null) {
				throw new BadFieldException("type", "type not found");
			}

			String indexName = ProviderService.extractIndexName(typeInfo, type);
			String indexType = ProviderService.extractIndexType(typeInfo, type);

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
		}
	}

	@GET
	@Path("/{contentId}")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	public Object getContent(@PathParam("type") String type, @PathParam("contentId") String contentId) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
			throw new RequiredFieldException("contentId");
		}
		if (type == null || SearchUtils.isBlank(type)) {
			throw new RequiredFieldException("type");
		}
		try {
			ProviderContentTypeInfo typeInfo = providerService.findContentType(type);
			if (typeInfo == null) {
				throw new BadFieldException("type");
			}

			String sysContentId = providerService.generateSysId(type, contentId);

			String indexName = ProviderService.extractIndexName(typeInfo, type);
			String indexType = ProviderService.extractIndexType(typeInfo, type);

			GetResponse getResponse = searchClientService.getClient().prepareGet(indexName, indexType, sysContentId)
					.execute().actionGet();

			if (!getResponse.isExists()) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			return createResponse(getResponse);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	/**
	 * Store new content into Searchisko.
	 * 
	 * This method fires {@link ContentStoredEvent}.
	 */
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

		if (content == null || content.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity("Some content for pushing must be defined").build();
		}

		if (type == null || SearchUtils.isBlank(type)) {
			throw new RequiredFieldException("type");
		}

		ProviderContentTypeInfo typeInfo = getTypeInfoWithManagePermissionCheck(type);

		String sysContentId = providerService.generateSysId(type, contentId);

		// check search subsystem configuration
		String indexName = ProviderService.extractIndexName(typeInfo, type);
		String indexType = ProviderService.extractIndexType(typeInfo, type);

		// fill some normalized fields - should be last step to avoid changing them via preprocessors
		content.put(ContentObjectFields.SYS_CONTENT_PROVIDER, typeInfo.getProviderName());
		content.put(ContentObjectFields.SYS_CONTENT_ID, contentId);
		content.put(ContentObjectFields.SYS_CONTENT_TYPE, type);
		content.put(ContentObjectFields.SYS_ID, sysContentId);
		content.put(ContentObjectFields.SYS_TYPE, ProviderService.extractSysType(typeInfo.getTypeDef(), type));
		content.put(ContentObjectFields.SYS_UPDATED, new Date());
		// Copy distinct data from content to normalized fields
		content.put(ContentObjectFields.SYS_TAGS, content.get(ContentObjectFields.TAGS));

		// Fill type of content from configuration
		if (content.containsKey(ContentObjectFields.SYS_CONTENT)) {
			content.put(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE,
					ProviderService.extractSysContentContentType(typeInfo.getTypeDef(), type));
		} else {
			content.remove(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE);
		}

		// Run preprocessors to manipulate other fields
		List<Map<String, String>> contentWarnings = providerService.runPreprocessors(type,
				ProviderService.extractPreprocessors(typeInfo, type), content);

		// Refill type of content from configuration if content was added in preprocessors
		if (content.containsKey(ContentObjectFields.SYS_CONTENT)
				&& !content.containsKey(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE)) {
			content.put(ContentObjectFields.SYS_CONTENT_CONTENT_TYPE,
					ProviderService.extractSysContentContentType(typeInfo.getTypeDef(), type));
		}

		if (ProviderService.extractPersist(typeInfo.getTypeDef())) {
			contentPersistenceService.store(sysContentId, type, content);
		}

		ContentBeforeIndexedEvent event1 = new ContentBeforeIndexedEvent(sysContentId, content);
		log.log(Level.FINE, "Going to fire event {0}", event1);
		eventBeforeIndexed.fire(event1);

		// Push to search subsystem
		IndexResponse ir = searchClientService.getClient().prepareIndex(indexName, indexType, sysContentId)
				.setSource(content).execute().actionGet();

		ContentStoredEvent event = new ContentStoredEvent(sysContentId, content);
		log.log(Level.FINE, "Going to fire event {0}", event);
		eventContentStored.fire(event);

		Map<String, Object> retJson = new LinkedHashMap<String, Object>();
		if (ir.getVersion() > 1) {
			retJson.put("status", "update");
			retJson.put("message", "Content was updated successfully.");
			if (contentWarnings != null && !contentWarnings.isEmpty())
				retJson.put("warnings", contentWarnings);
		} else {
			retJson.put("status", "insert");
			retJson.put("message", "Content was inserted successfully.");
			if (contentWarnings != null && !contentWarnings.isEmpty())
				retJson.put("warnings", contentWarnings);
		}
		return Response.ok(retJson).build();
	}

	/**
	 * Get info about requested content type with permission check for management.
	 * 
	 * @param type name to get info for
	 * @return type info, never null
	 * @throws BadFieldException if type is unknown
	 * @throws NotAuthorizedException if user has no permission to manage this type
	 */
	// TODO #77 UNIT TEST
	protected ProviderContentTypeInfo getTypeInfoWithManagePermissionCheck(String type) throws NotAuthorizedException,
			BadFieldException {
		ProviderContentTypeInfo typeInfo = providerService.findContentType(type);
		if (typeInfo == null) {
			throw new BadFieldException("type", "content type not found");
		}
		authenticationUtilService.checkProviderManagementPermission(securityContext, typeInfo.getProviderName());
		return typeInfo;
	}

	/**
	 * Delete content from Searchisko. This method fires {@link ContentDeletedEvent}.
	 */
	@DELETE
	@Path("/{contentId}")
	@ProviderAllowed
	public Object deleteContent(@PathParam("type") String type, @PathParam("contentId") String contentId,
			@QueryParam("ignore_missing") String ignoreMissing) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
			throw new RequiredFieldException("contentId");
		}
		if (type == null || SearchUtils.isBlank(type)) {
			throw new RequiredFieldException("type");
		}

		ProviderContentTypeInfo typeInfo = getTypeInfoWithManagePermissionCheck(type);

		String sysContentId = providerService.generateSysId(type, contentId);

		if (ProviderService.extractPersist(typeInfo.getTypeDef())) {
			contentPersistenceService.delete(sysContentId, type);
		}

		String indexName = ProviderService.extractIndexName(typeInfo, type);
		String indexType = ProviderService.extractIndexType(typeInfo, type);

		DeleteResponse dr = searchClientService.getClient().prepareDelete(indexName, indexType, sysContentId).execute()
				.actionGet();

		if (!dr.isNotFound()) {
			ContentDeletedEvent event = new ContentDeletedEvent(sysContentId);
			log.log(Level.FINE, "Going to fire event {0}", event);
			eventContentDeleted.fire(event);
		}

		if (dr.isNotFound() && !Boolean.parseBoolean(ignoreMissing)) {
			return Response.status(Status.NOT_FOUND).entity("Content not found to be deleted.").build();
		} else {
			return Response.ok("Content deleted successfully.").build();
		}
	}
}
