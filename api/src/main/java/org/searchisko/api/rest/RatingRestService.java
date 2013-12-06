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
import java.util.logging.Logger;

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

import org.elasticsearch.action.get.GetResponse;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.annotations.header.CORSSupport;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.persistence.jpa.model.Rating;
import org.searchisko.persistence.service.RatingPersistenceService;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

/**
 * REST API endpoint for 'Personalized Content Rating API'.
 * <p>
 * TODO _RATING unit tests
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
	protected Logger log;

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected RatingPersistenceService ratingPersistenceService;

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
	public Object getRating(@PathParam(QUERY_PARAM_ID) String contentSysId) {

		// validation
		if (contentSysId == null || contentSysId.isEmpty()) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		String currentContributorId = getAuthenticatedContributor(false);
		if (currentContributorId != null) {
			List<Rating> rl = ratingPersistenceService.getRatings(currentContributorId, contentSysId);
			if (rl != null && !rl.isEmpty()) {
				Map<String, Object> ret = ratingToJSON(rl.get(0));
				return ret;
			} else {
				return Response.status(Status.NOT_FOUND);
			}
		} else {
			return Response.status(Status.NOT_FOUND);
		}

	}

	/**
	 * Convert {@link Rating} object into JSON map.
	 * 
	 * @param rating to convert
	 * @return JSON map with rating informations
	 */
	protected Map<String, Object> ratingToJSON(Rating rating) {
		Map<String, Object> ret = new HashMap<>();
		ret.put(DATA_FIELD_RATING, rating.getRating());
		return ret;
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

		Set<String> contentSysIds = new HashSet<>();
		List<String> rawids = uriInfo.getQueryParameters().get(QUERY_PARAM_ID);
		if (rawids != null) {
			for (String rid : rawids) {
				rid = SearchUtils.trimToNull(rid);
				if (rid != null) {
					contentSysIds.add(rid);
				}
			}
		}

		if (!contentSysIds.isEmpty()) {
			List<Rating> rl = ratingPersistenceService.getRatings(currentContributorId,
					contentSysIds.toArray(new String[contentSysIds.size()]));
			if (rl != null && !rl.isEmpty()) {
				for (Rating r : rl) {
					ret.put(r.getContentId(), ratingToJSON(r));
				}
				return ret;
			} else {
				return Response.status(Status.NOT_FOUND);
			}
		}
		return ret;
	}

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@CORSSupport
	public Object postRating(@PathParam(QUERY_PARAM_ID) String contentSysId, Map<String, Object> requestContent) {

		String currentContributorId = getAuthenticatedContributor(true);

		// validation
		if (contentSysId == null || contentSysId.isEmpty()) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		Integer rating = null;
		try {
			rating = SearchUtils.getIntegerFromJsonMap(requestContent, DATA_FIELD_RATING);
		} catch (NumberFormatException e) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_RATING + " field must be number");
		}
		if (rating == null) {
			throw new RequiredFieldException(DATA_FIELD_RATING);
		}

		if (rating < 1 || rating > 5) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_RATING + " field must be number from 1 to 5");
		}

		// check if rated document exists
		String type = null;
		try {
			type = providerService.parseTypeNameFromSysId(contentSysId);
		} catch (IllegalArgumentException e) {
			log.fine("bad format or unknown type for content sys_id=" + contentSysId);
			return Response.status(Response.Status.NOT_FOUND).entity(QUERY_PARAM_ID + " is invalid").build();
		}

		Map<String, Object> typeDef = providerService.findContentTypeForDocumentSysId(type);
		if (typeDef == null) {
			log.fine("unknown type for content with sys_id=" + contentSysId);
			return Response.status(Response.Status.NOT_FOUND).entity(QUERY_PARAM_ID + " is invalid").build();
		}

		String indexName = ProviderService.extractIndexName(typeDef, type);
		String indexType = ProviderService.extractIndexType(typeDef, type);

		GetResponse getResponse = searchClientService.getClient().prepareGet(indexName, indexType, contentSysId).execute()
				.actionGet();
		if (!getResponse.isExists()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		// store rating
		ratingPersistenceService.rate(currentContributorId, contentSysId, rating);

		// count averages and store them into document
		RatingStats rs = ratingPersistenceService.countRatingStats(contentSysId);
		if (rs != null) {
			Map<String, Object> data = getResponse.getSource();
			data.put(ContentObjectFields.SYS_RATING_AVG, rs.getAverage());
			data.put(ContentObjectFields.SYS_RATING_NUM, rs.getNumber());
			searchClientService.getClient().prepareIndex(indexName, indexType, contentSysId).setSource(data).execute();
		} else {
			log.warning("Average rating is not found for content after ratring. sys_id=" + contentSysId);
		}
		Map<String, Object> ret = new HashMap<>();
		ret.put(ContentObjectFields.SYS_ID, contentSysId);
		ret.put(ContentObjectFields.SYS_RATING_AVG, rs.getAverage());
		ret.put(ContentObjectFields.SYS_RATING_NUM, rs.getNumber());
		return ret;
	}

	/**
	 * Get 'contributor id' for currently authenticated/logged in user.
	 * <p>
	 * TODO _RATING move this method to {@link RestServiceBase} so can be used in other services too!
	 * 
	 * @param forceCreate if <code>true</code> we need contributor id so backend should create it for logged in user if
	 *          not created yet. If <code>false</code> then we do not need it currently, so system can't create it but
	 *          return null instead.
	 * @return contributor id - can be null if
	 *         <code><forceCreate/code> is false and contributor record do not exists yet for current user.
	 * @throws NotAuthenticatedException in case contributor is not authenticated/logged in. This should never happen if
	 *           security interceptor is correctly implemented and configured for this class.
	 */
	protected String getAuthenticatedContributor(boolean forceCreate) throws NotAuthenticatedException {

		// TODO _RATING get logged in contributor id
		return "test <test@test.org>";
	}

}
