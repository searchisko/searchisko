/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.elasticsearch.action.get.GetResponse;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.rest.security.AuthenticationUtilService;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.service.SearchIndexMissingException;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.persistence.jpa.model.Rating;
import org.searchisko.persistence.service.RatingPersistenceService;
import org.searchisko.persistence.service.RatingPersistenceService.RatingStats;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.logging.Logger;

/**
 * REST API endpoint for 'Personalized Content Rating API'.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/rating")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({Role.ADMIN, Role.CONTRIBUTOR})
public class RatingRestService extends RestServiceBase {

	public static final String QUERY_PARAM_ID = "id";
	public static final String DATA_FIELD_RATING = "rating";

	@Inject
	protected Logger log;

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected RatingPersistenceService ratingPersistenceService;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Context
	protected SecurityContext securityContext;

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getRating(@PathParam(QUERY_PARAM_ID) String contentSysId) {

		contentSysId = SearchUtils.trimToNull(contentSysId);

		// validation
		if (contentSysId == null) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		String currentContributorId = authenticationUtilService.getAuthenticatedContributor(securityContext, false);
		if (currentContributorId != null) {
			List<Rating> rl = ratingPersistenceService.getRatings(currentContributorId, contentSysId);
			if (rl != null && !rl.isEmpty()) {
				Map<String, Object> ret = ratingToJSON(rl.get(0));
				return ret;
			} else {
				return Response.status(Status.NOT_FOUND).build();
			}
		} else {
			return Response.status(Status.NOT_FOUND).build();
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
	public Map<String, Object> getRatings(@Context UriInfo uriInfo) {

		Map<String, Object> ret = new HashMap<>();

		String currentContributorId = authenticationUtilService.getAuthenticatedContributor(securityContext, false);
		if (currentContributorId == null || uriInfo == null) {
			return ret;
		}

		Set<String> contentSysIds = new LinkedHashSet<>();
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
			}
		} else {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}
		return ret;
	}

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object postRating(@PathParam(QUERY_PARAM_ID) String contentSysId, Map<String, Object> requestContent) {

		String currentContributorId = authenticationUtilService.getAuthenticatedContributor(securityContext, true);

		contentSysId = SearchUtils.trimToNull(contentSysId);

		// validation
		if (contentSysId == null) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		Integer rating = null;
		try {
			rating = SearchUtils.getIntegerFromJsonMap(requestContent, DATA_FIELD_RATING);
		} catch (NumberFormatException e) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_RATING + " field must be number").build();
		}
		if (rating == null) {
			throw new RequiredFieldException(DATA_FIELD_RATING);
		}

		if (rating < 1 || rating > 5) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_RATING + " field must be number from 1 to 5")
					.build();
		}

		// check if rated document exists
		String type = null;
		try {
			type = providerService.parseTypeNameFromSysId(contentSysId);
		} catch (IllegalArgumentException e) {
			log.fine("bad format or unknown type for content sys_id=" + contentSysId);
			return Response.status(Response.Status.BAD_REQUEST).entity(QUERY_PARAM_ID + " format is invalid").build();
		}

		ProviderContentTypeInfo typeInfo = providerService.findContentType(type);
		if (typeInfo == null) {
			log.fine("unknown type for content with sys_id=" + contentSysId);
			return Response.status(Response.Status.NOT_FOUND).entity("content type is unknown").build();
		}

		String indexName = ProviderService.extractIndexName(typeInfo, type);
		String indexType = ProviderService.extractIndexType(typeInfo, type);

		try {
			GetResponse getResponse = searchClientService.performGet(indexName, indexType, contentSysId);
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
				searchClientService.performPutAsync(indexName, indexType, contentSysId, data);
			} else {
				log.warning("Average rating is not found for content after ratring. sys_id=" + contentSysId);
			}
			Map<String, Object> ret = new HashMap<>();
			ret.put(ContentObjectFields.SYS_ID, contentSysId);
			if (rs != null) {
				ret.put(ContentObjectFields.SYS_RATING_AVG, rs.getAverage());
				ret.put(ContentObjectFields.SYS_RATING_NUM, rs.getNumber());
			}
			return ret;
		} catch (SearchIndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

}
