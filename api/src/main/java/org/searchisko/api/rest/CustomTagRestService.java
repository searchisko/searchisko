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
import org.apache.commons.lang.StringUtils;

import org.elasticsearch.action.get.GetResponse;
import org.searchisko.api.ContentObjectFields;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.CustomTagService;
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
	protected CustomTagService customTagService;

	@Inject
	protected CustomTagPersistenceService customTagPersistenceService;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;


	/**
	 * Custom authentication check if user is in role {@link org.searchisko.api.security.Role#TAGS_MANAGER}
	 * or {@link org.searchisko.api.security.Role#ADMIN} or <code>tags_manager_contentType</code>
	 *
	 * @param contentType - check permission for content type
	 *
	 * @throws NotAuthorizedException if user doesn't have required role
	 */
	protected void checkIfUserAuthenticated(String contentType) throws NotAuthorizedException {
		if (!(authenticationUtilService.isUserInAnyOfRoles(true, Role.TAGS_MANAGER, Role.TAGS_MANAGER + "_" + contentType))) {
			throw new NotAuthorizedException("User Not Authorized for Tagging API");
		}
	}
        /**
	 * Custom authentication check if user is in role {@link org.searchisko.api.security.Role#TAGS_MANAGER}
	 * or {@link org.searchisko.api.security.Role#ADMIN}
	 *
	 *
	 * @throws NotAuthorizedException if user doesn't have required role
	 */
	protected void checkIfUserAuthenticated() throws NotAuthorizedException {
		if (!(authenticationUtilService.isUserInAnyOfRoles(true, Role.TAGS_MANAGER))) {
			throw new NotAuthorizedException("User Not Authorized for Tagging API");
		}
	}

	@GET
	@Path("/{" + QUERY_PARAM_ID + "}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getTagsByContent(@PathParam(QUERY_PARAM_ID) String contentSysId) {
		contentSysId = SearchUtils.trimToNull(contentSysId);
		// validation
		if (contentSysId == null) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
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

		checkIfUserAuthenticated(providerService.parseTypeNameFromSysId(contentSysId));

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
		List<String> labels = new ArrayList<>();
		for (Tag tag : tags) {
			labels.add(tag.getTagLabel());
		}
		result.put(DATA_FIELD_TAGGING, labels);
		return result;
	}

	@GET
	@Path("/type/{" + QUERY_PARAM_ID + "}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getTagsByContentType(@PathParam(QUERY_PARAM_ID) String contentType) {
		contentType = SearchUtils.trimToNull(contentType);
		// validation
		if (contentType == null) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
		}

		ProviderContentTypeInfo typeInfo = providerService.findContentType(contentType);
		if (typeInfo == null) {
			log.fine("unknown type id=" + contentType);
			return Response.status(Response.Status.NOT_FOUND).entity("content type is unknown").build();
		}

		checkIfUserAuthenticated(contentType);

		List<Tag> tagList = customTagPersistenceService.getTagsByContentType(contentType);
		if (tagList != null && !tagList.isEmpty()) {
			Map<String, Object> result = tagsToJSON(tagList);
			return result;
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@POST
	@Path("/{" + QUERY_PARAM_ID + "}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object postTag(@PathParam(QUERY_PARAM_ID) String contentSysId, Map<String, Object> requestContent) {
		contentSysId = SearchUtils.trimToNull(contentSysId);
		// validation
		if ((contentSysId == null) || (requestContent == null)) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
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

		checkIfUserAuthenticated(providerService.parseTypeNameFromSysId(contentSysId));

		String currentContributorId = authenticationUtilService.getAuthenticatedContributor(true);

		String tag = null;

		if (requestContent.get(DATA_FIELD_TAGGING) == null) {
			throw new RequiredFieldException(DATA_FIELD_TAGGING);
		}

		if (!(requestContent.get(DATA_FIELD_TAGGING) instanceof String)) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_TAGGING + " field must be text string").build();
		}
		tag = StringUtils.trim((String) requestContent.get(DATA_FIELD_TAGGING));

		if (tag.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_TAGGING + " field cannot be empty").build();
		}

		// elastic search indices
		String indexName = ProviderService.extractIndexName(typeInfo, type);
		String indexType = ProviderService.extractIndexType(typeInfo, type);

		try {
			GetResponse getResponse = searchClientService.performGet(indexName, indexType, contentSysId);
			if (!getResponse.isExists()) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			// check for same tag in provider tags
			Map<String, Object> source = getResponse.getSource();
			SortedSet<String> providerTags = new TreeSet(String.CASE_INSENSITIVE_ORDER);
			List<String> providersList = (List<String>) source.get(ContentObjectFields.TAGS);
			if (providersList != null) {
				providerTags.addAll(providersList);
			}

			boolean created;
			if (!(providerTags.contains(tag))) {
				// store tag
				Tag tagObject = new Tag();
				tagObject.setContentId(contentSysId);
				tagObject.setContributorId(currentContributorId);
				tagObject.setTagLabel(tag);
				created = customTagPersistenceService.createTag(tagObject);
				if (created) {
					customTagService.updateSysTagsField(source);
					searchClientService.performPut(indexName, indexType, contentSysId, source);
				}
			} else
				created = false;

			return Response.status(created ? Response.Status.CREATED : Response.Status.OK).build();
		} catch (SearchIndexMissingException e) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
	}


	@DELETE
	@Path("/{" + QUERY_PARAM_ID + "}/_all")
	public Object deleteTagsForContent(@PathParam(QUERY_PARAM_ID) String contentSysId) {
		contentSysId = SearchUtils.trimToNull(contentSysId);

		// validation
		if (contentSysId == null) {
			throw new RequiredFieldException(QUERY_PARAM_ID);
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

		checkIfUserAuthenticated();

		// delete tags from custom tags
		customTagPersistenceService.deleteTagsForContent(contentSysId);

		// delete tags from SYS_TAG field (update SYS_TAG field)
		String indexName = ProviderService.extractIndexName(typeInfo, type);
		String indexType = ProviderService.extractIndexType(typeInfo, type);
		GetResponse getResponse;
		try {
			getResponse = searchClientService.performGet(indexName, indexType, contentSysId);
		} catch (SearchIndexMissingException ex) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		if (!getResponse.isExists()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Map<String, Object> source = getResponse.getSource();
		customTagService.updateSysTagsField(source);
		searchClientService.performPut(indexName, indexType, contentSysId, source);

		return Response.status(Status.OK).build();
	}

	@DELETE
	@Path("/{" + QUERY_PARAM_ID + "}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object deleteTag(@PathParam(QUERY_PARAM_ID) String contentSysId, Map<String, Object> requestContent) {
		contentSysId = SearchUtils.trimToNull(contentSysId);

		// validation
		if ((contentSysId == null) || (requestContent == null)){
			throw new RequiredFieldException(QUERY_PARAM_ID);
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

		checkIfUserAuthenticated();

		String tagLabel = null;
		try {
			tagLabel = StringUtils.trim((String) requestContent.get(DATA_FIELD_TAGGING));
		} catch (ClassCastException e) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_TAGGING + " field must be text string").build();
		}
		if (tagLabel == null) {
			throw new RequiredFieldException(DATA_FIELD_TAGGING);
		}

		if (tagLabel.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity(DATA_FIELD_TAGGING + " field cannot be empty").build();
		}

		// delete tag from custom tags
		customTagPersistenceService.deleteTag(contentSysId, tagLabel);


		// delete tag from SYS_TAG field (update SYS_TAG field)
		String indexName = ProviderService.extractIndexName(typeInfo, type);
		String indexType = ProviderService.extractIndexType(typeInfo, type);
		GetResponse getResponse;
		try {
			getResponse = searchClientService.performGet(indexName, indexType, contentSysId);
		} catch (SearchIndexMissingException ex) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		if (!getResponse.isExists()) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		Map<String, Object> source = getResponse.getSource();
		customTagService.updateSysTagsField(source);
		searchClientService.performPut(indexName, indexType, contentSysId, source);

		return Response.status(Status.OK).build();
	}

}
