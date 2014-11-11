/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.logging.Logger;

import org.elasticsearch.action.get.GetResponse;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.service.SearchIndexMissingException;
import org.searchisko.api.util.SearchUtils;
import org.searchisko.persistence.jpa.model.Tag;
import org.searchisko.persistence.service.CustomTagPersistenceService;

/**
 * REST API endpoint for 'Custom Tag API'.
 *
 * @author Jiri Mauritz (jirmauritz at gmail dot com)
 */
@RequestScoped
@Path("/tagging")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
// Standard Roles are not used because it's designed to return 403 if user is not authenticated.
//@RolesAllowed({Role.ADMIN, Role.CONTRIBUTOR})
public class CustomTagRestService extends RestServiceBase {

	public static final String QUERY_PARAM_ID = "id";
	public static final String DATA_FIELD_TAGGING = "tag";

	@Inject
	protected Logger log;

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected CustomTagPersistenceService customTagPersistenceService;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Context
	protected SecurityContext securityContext;

	/**
	 * Custom authentication check if user is in role ${@link org.searchisko.api.security.Role#CONTRIBUTOR}
	 * or ${@link org.searchisko.api.security.Role#ADMIN}
	 *
	 * @throws NotAuthorizedException if user doesn't have required role
	 */
	protected void checkIfUserAuthenticated() throws NotAuthorizedException {
		if (!(securityContext.isUserInRole(Role.CONTRIBUTOR) || securityContext.isUserInRole(Role.ADMIN))) {
			throw new NotAuthorizedException("User Not Authorized for Content Rating API");
		}
	}

	@GET
	@Path("/{" + QUERY_PARAM_ID + "}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getTagsByContent(@PathParam(QUERY_PARAM_ID) String contentSysId) {
		checkIfUserAuthenticated();

		contentSysId = SearchUtils.trimToNull(contentSysId);

		// validation
		if (contentSysId == null) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		List<Tag> tagList = customTagPersistenceService.getTagsByContent(contentSysId);
		if (tagList != null && !tagList.isEmpty()) {
			Map<String, Object> result = tagsToJSON(tagList);
			return result;
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	/**
	 * Convert {@link Tag} object into JSON map.
	 *
	 * @param tags list of tags to convert
	 * @return JSON map with information about tag
	 */
	protected Map<String, Object> tagsToJSON(List<Tag> tags) {
		Map<String, Object> result = new HashMap<>();
		result.put("tags", tags);
		return result;
	}

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getAllTags() {
		checkIfUserAuthenticated();

		List<Tag> tagList = customTagPersistenceService.getAllTags();
		if (tagList != null && !tagList.isEmpty()) {
			Map<String, Object> result = tagsByContentToJSON(tagList);
			return result;
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	/**
	 * Convert {@link Tag} object sorted by content into JSON map.
	 *
	 * @param tags list of tags to convert
	 * @return JSON map with information about tag
	 */
	protected Map<String, Object> tagsByContentToJSON(List<Tag> tags) {
		Map<String, Object> result = new HashMap<>();
		for (Tag tag : tags) {
			if (result.containsKey(tag.getContentId())) {
				((List<String>) result.get(tag.getContentId())).add(tag.getTag());
			} else {
				List<String> tl = new ArrayList<>();
				tl.add(tag.getTag());
				result.put(tag.getContentId(), tl);
			}
		}
		return result;
	}

	@POST
	@Path("/{" + QUERY_PARAM_ID + "}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object postTag(@PathParam(QUERY_PARAM_ID) String contentSysId, Map<String, Object> requestContent) {
		checkIfUserAuthenticated();

		String currentContributorId = authenticationUtilService.getAuthenticatedContributor(true);


		contentSysId = SearchUtils.trimToNull(contentSysId);

		// validation
		if (contentSysId == null) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		String tag = null;
		try {
			tag = (String) requestContent.get(DATA_FIELD_TAGGING);
		} catch (ClassCastException e) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_TAGGING + " field must be text string").build();
		}
		if (tag == null) {
			throw new RequiredFieldException(DATA_FIELD_TAGGING);
		}

		if (tag.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_TAGGING + " field cannot be empty").build();
		}

		// check if tagged document exists
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

			// store tag
			Tag tagObject = new Tag();
			tagObject.setContentId(contentSysId);
			tagObject.setContributorId(currentContributorId);
			tagObject.setTag(tag);
			customTagPersistenceService.createTag(tagObject);

			return createResponse(getResponse);
		} catch (SearchIndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}

}
