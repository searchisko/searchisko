/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.indices.IndexMissingException;
import org.searchisko.api.annotations.header.CORSSupport;
import org.searchisko.api.annotations.security.GuestAllowed;
import org.searchisko.api.model.QuerySettings;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.SearchService;
import org.searchisko.api.service.StatsRecordType;
import org.searchisko.api.util.QuerySettingsParser;
import org.searchisko.api.util.SearchUtils;

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
	protected QuerySettingsParser querySettingsParser;

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	@CORSSupport
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
			Map<String, String> af = searchService.getSearchResponseAdditionalFields(querySettings);
			af.put("uuid", responseUuid);
			return createResponse(searchResponse, af);
		} catch (IllegalArgumentException e) {
            throw new BadFieldException("unknown", e);
		} catch (IndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
//		} catch (Exception e) {
//			return createErrorResponse(e);
		}
	}

	@OPTIONS
	@Path("/{search_result_uuid}/{hit_id}")
	@GuestAllowed
	@CORSSupport(allowedMethods = { CORSSupport.PUT, CORSSupport.POST })
	public Object writeSearchHitUsedStatisticsRecordOPTIONS() {
		return Response.ok().build();
	}

	@PUT
	@Path("/{search_result_uuid}/{hit_id}")
	@GuestAllowed
	@CORSSupport
	public Object writeSearchHitUsedStatisticsRecordPUT(@PathParam("search_result_uuid") String uuid,
			@PathParam("hit_id") String contentId, @QueryParam("session_id") String sessionId) {
		return writeSearchHitUsedStatisticsRecord(uuid, contentId, sessionId);
	}

	@POST
	@Path("/{search_result_uuid}/{hit_id}")
	@GuestAllowed
	@CORSSupport
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
//		try {
			boolean result = searchService.writeSearchHitUsedStatisticsRecord(uuid, contentId, sessionId);
			return Response.ok(result ? "statistics record accepted" : "statistics record ignored").build();
//		} catch (Exception e) {
//			return createErrorResponse(e);
//		}
	}

}
