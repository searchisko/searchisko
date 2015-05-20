/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.indices.IndexMissingException;
import org.searchisko.api.audit.annotation.AuditId;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.RegisteredQueryService;
import org.searchisko.api.service.SearchService;
import org.searchisko.api.service.StatsRecordType;
import org.searchisko.api.util.QuerySettingsParser;
import org.searchisko.api.util.SearchUtils;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.searchisko.api.util.SearchUtils.collapseURLParams;

/**
 * Search REST API.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukas Vlcek
 * 
 */
@RequestScoped
@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
public class SearchRestService extends RestServiceBase {

	@Inject
	protected SearchService searchService;

	@Inject
	protected RegisteredQueryService registeredQueryService;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Inject
	protected QuerySettingsParser querySettingsParser;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	public Object search(@Context UriInfo uriInfo) {

		QuerySettings querySettings = null;
		try {

			if (uriInfo == null) {
				throw new BadFieldException("uriInfo");
			}
			MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
			querySettings = querySettingsParser.parseUriParams(params);
			String responseUuid = UUID.randomUUID().toString();

			SearchResponse searchResponse = searchService.performSearch(querySettings, responseUuid, StatsRecordType.SEARCH);
			Map<String, String> af = searchService.getIntervalValuesForDateHistogramAggregations(querySettings);
			af.put("uuid", responseUuid);
			return createResponse(searchResponse, af);
		} catch (IllegalArgumentException e) {
			throw new BadFieldException("unknown", e);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	public Object searchRegisteredQuery(@PathParam("id") @AuditId String id, @Context UriInfo uriInfo) {
		try {
			if ((id = SearchUtils.trimToNull(id)) == null) {
				throw new RequiredFieldException("id");
			}

			Map<String, Object> registeredQuery = registeredQueryService.get(id);
			if (registeredQuery == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			@SuppressWarnings("unchecked")
			List<String> roles = (List<String>) registeredQuery.get(RegisteredQueryService.FIELD_ALLOWED_ROLES);

			boolean allowed = true;
			if (roles != null && !roles.isEmpty() && !authenticationUtilService.isUserInRole(Role.ADMIN)) {
				if (!authenticationUtilService.isUserInAnyOfRoles(false, roles)) {
					allowed = false;
				}
			}

			if (allowed) {
				if (uriInfo == null) {
					throw new BadFieldException("uriInfo");
				}
				MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
				QuerySettings.Filters filters = querySettingsParser.parseUriParams(params).getFilters();
				Map<String, Object> collapsedParams = collapseURLParams(params);
				String responseUuid = UUID.randomUUID().toString();

				SearchResponse searchResponse = searchService.performSearchTemplate(id, collapsedParams, filters);
				Map<String, String> af = new HashMap<>();
				af.put("uuid", responseUuid);
				return createResponse(searchResponse, af);
			} else {
				throw new NotAuthorizedException("Client missing required role to execute this registered query.");
			}
		} catch (IllegalArgumentException e) {
			throw new BadFieldException("unknown", e);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

	@PUT
	@Path("/{search_result_uuid}/{hit_id}")
	@PermitAll
	public Object writeSearchHitUsedStatisticsRecordPUT(@PathParam("search_result_uuid") String uuid,
			@PathParam("hit_id") String contentId, @QueryParam("session_id") String sessionId) {
		return writeSearchHitUsedStatisticsRecord(uuid, contentId, sessionId);
	}

	@POST
	@Path("/{search_result_uuid}/{hit_id}")
	@PermitAll
	public Object writeSearchHitUsedStatisticsRecordPOST(@PathParam("search_result_uuid") String uuid,
			@PathParam("hit_id") String contentId, @QueryParam("session_id") String sessionId) {
		return writeSearchHitUsedStatisticsRecord(uuid, contentId, sessionId);
	}

	protected Object writeSearchHitUsedStatisticsRecord(@PathParam("search_result_uuid") String uuid,
			@PathParam("hit_id") String contentId, @QueryParam("session_id") String sessionId) {

		if ((uuid = SearchUtils.trimToNull(uuid)) == null) {
			throw new RequiredFieldException("search_result_uuid");
		}
		if ((contentId = SearchUtils.trimToNull(contentId)) == null) {
			throw new RequiredFieldException("hit_id");
		}
		sessionId = SearchUtils.trimToNull(sessionId);
		boolean result = searchService.writeSearchHitUsedStatisticsRecord(uuid, contentId, sessionId);
		// TODO: Search REST API: Return Bad Request Return code if result is false.
		return Response.ok(result ? "statistics record accepted" : "statistics record ignored").build();
	}
}
