/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.searchisko.api.annotations.header.CORSSupport;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.util.SearchUtils;

/**
 * REST API endpoint for 'Personalized Content Rating API'.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/rating")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// TODO _RATING REST endpoint security to logged in contributors only
public class RatingRestService extends RestServiceBase {

	private static final String QUERY_PARAM_ID = "id";
	private static final String DATA_FIELD_RATING = "rating";

	@Inject
	protected SearchClientService searchClientService;

	@OPTIONS
	@Path("/{id}")
	@CORSSupport(allowedMethods = { CORSSupport.GET, CORSSupport.POST })
	public Object writeSearchHitUsedStatisticsRecordOPTIONS() {
		return Response.ok().build();
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@CORSSupport
	public Object getRating(@PathParam(QUERY_PARAM_ID) String id) {

		// validation
		if (id == null || id.isEmpty()) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		String currentContributorId = getAuthenticatedContributor(false);
		if (currentContributorId != null) {
			// get logged in contributor id

			// get rating and return if exists, return 404 if not exists

			// TODO _RATING get rating REST api
			return null;
		} else {
			return Response.status(Status.NOT_FOUND);
		}

	}

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@CORSSupport
	public Object getRatings(@Context UriInfo uriInfo) {

		Map<String, Object> ret = new HashMap<>();

		String currentContributorId = getAuthenticatedContributor(false);
		if (currentContributorId == null) {
			return ret;
		}

		Set<String> ids = new HashSet<>();
		List<String> rawids = uriInfo.getQueryParameters().get(QUERY_PARAM_ID);
		if (rawids != null) {
			for (String rid : rawids) {
				rid = SearchUtils.trimToNull(rid);
				if (rid != null) {
					ids.add(rid);
				}
			}
		}

		if (!ids.isEmpty()) {
			// get logged in contributor id

			// get ratings for all id's and return Map

			// TODO _RATING get rating REST api
		}
		return ret;
	}

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@CORSSupport
	public void postRating(@PathParam(QUERY_PARAM_ID) String id) {

		// validation
		if (id == null || id.isEmpty()) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		String currentContributorId = getAuthenticatedContributor(true);

		// get document by id

		// write rating record

		// count averages and store them into document

		// TODO _RATING post REST api
	}

	/**
	 * Get 'contributor id' for currently authenticated/logged in user.
	 * <p>
	 * TODO _RATING move this method to {@link RestServiceBase} so can be used in other services too!
	 * 
	 * @param forceCreate if <code>true</code> we need contributor id so backend should create it for logged in user if
	 *          not created yet.
	 * @return contributor id - can be null if
	 *         <code><forceCreate/code> is false and contributor record do not exists yet for current user.
	 * @throws NotAuthenticatedException in case contributor is not authenticated/logged in. This should never happen if
	 *           security interceptor is correctly implemented and configured for this class.
	 */
	protected String getAuthenticatedContributor(boolean forceCreate) throws NotAuthenticatedException {

		// TODO _RATING get logged in contributor id
		return null;
	}

}
