/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.sort.SortOrder;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.audit.annotation.Audit;
import org.searchisko.api.audit.annotation.AuditContent;
import org.searchisko.api.audit.annotation.AuditId;
import org.searchisko.api.events.ContentBeforeIndexedEvent;
import org.searchisko.api.events.ContentDeletedEvent;
import org.searchisko.api.events.ContentStoredEvent;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.OperationUnavailableException;
import org.searchisko.api.rest.exception.PreprocessorInvalidDataException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.ContentManipulationLockService;
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
@RolesAllowed({ Role.ADMIN, Role.PROVIDER })
public class ContentRestService extends RestServiceBase {

	static final String RETFIELD_WARNINGS = "warnings";

	static final String RETFIELD_MESSAGE = "message";

	static final String RETFIELD_STATUS = "status";

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected ContentPersistenceService contentPersistenceService;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Inject
	protected ContentManipulationLockService contentManipulationLockService;

	@Inject
	protected Event<ContentStoredEvent> eventContentStored;

	@Inject
	protected Event<ContentDeletedEvent> eventContentDeleted;

	@Inject
	protected Event<ContentBeforeIndexedEvent> eventBeforeIndexed;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
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
	@PermitAll
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
	 * This method fires {@link ContentBeforeIndexedEvent} and {@link ContentStoredEvent}.
	 */
	@POST
	@Path("/{contentId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object pushContent(@PathParam("type") String type, @PathParam("contentId") String contentId,
			Map<String, Object> content) throws PreprocessorInvalidDataException {

		ProviderContentTypeInfo typeInfo = getTypeInfoWithManagePermissionCheck(type);

		PushContentImplRet pcir = pushContentImpl(typeInfo, contentId, content);

		// Push to search subsystem
		IndexResponse ir = pcir.irb.execute().actionGet();

		ContentStoredEvent event = new ContentStoredEvent(pcir.sysContentId, content);
		log.log(Level.FINE, "Going to fire event {0}", event);
		eventContentStored.fire(event);

		Map<String, Object> retJson = new LinkedHashMap<String, Object>();
		processIndexResponse(ir, retJson);
		if (pcir.contentWarnings != null && !pcir.contentWarnings.isEmpty())
			retJson.put(RETFIELD_WARNINGS, pcir.contentWarnings);
		return Response.ok(retJson).build();
	}

	private void processIndexResponse(IndexResponse ir, Map<String, Object> retJson) {
		if (ir.getVersion() > 1) {
			retJson.put(RETFIELD_STATUS, "update");
			retJson.put(RETFIELD_MESSAGE, "Content updated successfully.");
		} else {
			retJson.put(RETFIELD_STATUS, "insert");
			retJson.put(RETFIELD_MESSAGE, "Content inserted successfully.");
		}
	}

	/**
	 * Store bulk of new content into Searchisko.
	 * 
	 * This method fires series of {@link ContentBeforeIndexedEvent} and {@link ContentStoredEvent}.
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object pushContentBulk(@PathParam("type") String type, Map<String, Object> contentStructure) {
		ProviderContentTypeInfo typeInfo = getTypeInfoWithManagePermissionCheck(type);

		Map<String, Object> ret = new LinkedHashMap<>();

		if (contentStructure.isEmpty())
			return ret;

		BulkRequestBuilder brb = searchClientService.getClient().prepareBulk();
		List<String> ids = new ArrayList<>();
		Map<String, PushContentImplRet> pcis = new LinkedHashMap<>();

		for (String contentId : contentStructure.keySet()) {
			Object o = contentStructure.get(contentId);
			if (!(o instanceof Map)) {
				Map<String, Object> retitem = new LinkedHashMap<>();
				retitem.put(RETFIELD_STATUS, "error");
				retitem.put(RETFIELD_MESSAGE, "content must be JSON structure");
				ret.put(contentId, retitem);
			} else {
				@SuppressWarnings("unchecked")
				Map<String, Object> content = (Map<String, Object>) o;
				try {
					PushContentImplRet pcir = pushContentImpl(typeInfo, contentId, content);
					brb.add(pcir.irb);
					ids.add(contentId);
					pcis.put(contentId, pcir);
				} catch (RequiredFieldException | BadFieldException | PreprocessorInvalidDataException e) {
					Map<String, Object> retitem = new LinkedHashMap<>();
					retitem.put(RETFIELD_STATUS, "error");
					retitem.put(RETFIELD_MESSAGE, e.getMessage());
					ret.put(contentId, retitem);
				}
			}
		}

		if (!ids.isEmpty()) {
			BulkResponse br = brb.execute().actionGet();

			int i = 0;
			for (BulkItemResponse bri : br.getItems()) {
				String contentId = ids.get(i);
				Map<String, Object> retitem = new LinkedHashMap<>();
				ret.put(contentId, retitem);
				if (!bri.isFailed()) {
					PushContentImplRet pcir = pcis.get(contentId);
					ContentStoredEvent event = new ContentStoredEvent(pcir.sysContentId, pcir.content);
					log.log(Level.FINE, "Going to fire event {0}", event);
					eventContentStored.fire(event);

					processIndexResponse((IndexResponse) bri.getResponse(), retitem);
					if (pcir.contentWarnings != null && !pcir.contentWarnings.isEmpty())
						retitem.put(RETFIELD_WARNINGS, pcir.contentWarnings);
				} else {
					retitem.put(RETFIELD_STATUS, "error");
					retitem.put(RETFIELD_MESSAGE, bri.getFailureMessage());
				}
				i++;
			}
		}
		return ret;

	}

	public PushContentImplRet pushContentImpl(ProviderContentTypeInfo typeInfo, String contentId,
			Map<String, Object> content) throws RequiredFieldException, BadFieldException, PreprocessorInvalidDataException {

		String type = typeInfo.getTypeName();

		// content validation
		if (contentId == null || contentId.isEmpty()) {
			throw new RequiredFieldException("contentId");
		}

		if (contentId.startsWith("_") || contentId.contains(",") || contentId.contains("*")) {
			throw new BadFieldException("contentId", "contentId can't start with underscore or contain comma, star");
		}

		if (content == null || content.isEmpty()) {
			throw new BadFieldException("content", "Some content for pushing must be defined");
		}

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

		processFieldSysVisibleForRoles(content);

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

		IndexRequestBuilder irb = searchClientService.getClient().prepareIndex(indexName, indexType, sysContentId)
				.setSource(content);
		return new PushContentImplRet(irb, contentWarnings, sysContentId, contentId, content);
	}

	protected void processFieldSysVisibleForRoles(Map<String, Object> content) {
		try {
			List<String> vr = SearchUtils.getListOfStringsFromJsonMap(content, ContentObjectFields.SYS_VISIBLE_FOR_ROLES);
			if (vr != null) {
				content.put(ContentObjectFields.SYS_VISIBLE_FOR_ROLES, vr);
			} else {
				content.remove(ContentObjectFields.SYS_VISIBLE_FOR_ROLES);
			}
		} catch (SettingsException e) {
			throw new BadFieldException(ContentObjectFields.SYS_VISIBLE_FOR_ROLES);
		}
	}

	protected static final class PushContentImplRet {
		IndexRequestBuilder irb;
		List<Map<String, String>> contentWarnings;
		String sysContentId;
		String contentId;
		Map<String, Object> content;

		public PushContentImplRet(IndexRequestBuilder irb, List<Map<String, String>> contentWarnings, String sysContentId,
				String contentId, Map<String, Object> content) {
			super();
			this.irb = irb;
			this.contentWarnings = contentWarnings;
			this.sysContentId = sysContentId;
			this.contentId = contentId;
			this.content = content;
		}
	}

	/**
	 * Get info about requested content type with permission check for management and check for API lockdown.
	 * 
	 * @param type name to get info for
	 * @return type info, never null
	 * @throws BadFieldException if type is unknown
	 * @throws NotAuthorizedException if user has no permission to manage this type
	 * @throws OperationUnavailableException if API is locked down
	 * @see ContentManipulationLockService
	 */
	protected ProviderContentTypeInfo getTypeInfoWithManagePermissionCheck(String type) throws NotAuthorizedException,
			BadFieldException, OperationUnavailableException {

		if (type == null || SearchUtils.isBlank(type)) {
			throw new RequiredFieldException("type");
		}

		ProviderContentTypeInfo typeInfo = providerService.findContentType(type);
		if (typeInfo == null) {
			throw new BadFieldException("type", "content type not found");
		}
		authenticationUtilService.checkProviderManagementPermission(typeInfo.getProviderName());

		if (contentManipulationLockService.isLockedForProvider(typeInfo.getProviderName())) {
			throw new OperationUnavailableException("Content manipulation API is locked down by administrator");
		}

		return typeInfo;
	}

	/**
	 * Delete content from Searchisko. This method fires {@link ContentDeletedEvent}.
	 */
	@DELETE
	@Path("/{contentId}")
	@Audit
	public Object deleteContent(@PathParam("type") String type, @PathParam("contentId") @AuditId String contentId,
			@QueryParam("ignore_missing") String ignoreMissing) {

		// validation
		if (contentId == null || contentId.isEmpty()) {
			throw new RequiredFieldException("contentId");
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

		if (dr.isFound()) {
			ContentDeletedEvent event = new ContentDeletedEvent(sysContentId);
			log.log(Level.FINE, "Going to fire event {0}", event);
			eventContentDeleted.fire(event);
		}

		if (!dr.isFound() && !Boolean.parseBoolean(ignoreMissing)) {
			return Response.status(Status.NOT_FOUND).entity("Content not found to be deleted.").build();
		} else {
			return Response.ok("Content deleted successfully.").build();
		}
	}

	/**
	 * Bulk delete of content from Searchisko. This method fires {@link ContentDeletedEvent}.
	 */
	@DELETE
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Audit
	public Object deleteContentBulk(@PathParam("type") String type, @AuditContent Map<String, Object> content) {

		ProviderContentTypeInfo typeInfo = getTypeInfoWithManagePermissionCheck(type);

		Object o = content == null ? null : content.get("id");
		if (o == null) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Request content must contain 'id' field with array of strings with content identifiers to delete")
					.build();
		}
		if (!(o instanceof List)) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Request content 'id' field must be an array of strings with identifiers").build();
		}

		@SuppressWarnings("unchecked")
		List<String> ids = (List<String>) o;

		Map<String, String> ret = new LinkedHashMap<>();
		if (ids.isEmpty())
			return ret;

		String indexName = ProviderService.extractIndexName(typeInfo, type);
		String indexType = ProviderService.extractIndexType(typeInfo, type);

		BulkRequestBuilder brb = searchClientService.getClient().prepareBulk();

		List<String> sysIds = new ArrayList<>();
		for (String contentId : ids) {
			String sysContentId = providerService.generateSysId(type, contentId);
			sysIds.add(sysContentId);

			if (ProviderService.extractPersist(typeInfo.getTypeDef())) {
				contentPersistenceService.delete(sysContentId, type);
			}

			brb.add(searchClientService.getClient().prepareDelete(indexName, indexType, sysContentId));
		}

		BulkResponse br = brb.execute().actionGet();

		int i = 0;
		for (BulkItemResponse bri : br.getItems()) {
			String contentId = ids.get(i);
			if (!bri.isFailed()) {
				DeleteResponse dr = bri.getResponse();
				if (dr.isFound()) {
					ContentDeletedEvent event = new ContentDeletedEvent(sysIds.get(i));
					log.log(Level.FINE, "Going to fire event {0}", event);
					eventContentDeleted.fire(event);
					ret.put(contentId, "ok");
				} else {
					ret.put(contentId, "not_found");
				}
			} else {
				ret.put(contentId, "error: " + bri.getFailureMessage());
			}
			i++;
		}

		return ret;
	}
}
