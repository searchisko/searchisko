/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.Map;

import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.indexer.EsRiverJiraIndexerHandler;
import org.searchisko.api.indexer.EsRiverRemoteIndexerHandler;
import org.searchisko.api.indexer.IndexerHandler;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.rest.security.AuthenticationUtilService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.util.SearchUtils;

/**
 * REST API for Indexer related operations.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/indexer/{type}")
@Produces(MediaType.APPLICATION_JSON)
@ProviderAllowed
public class IndexerRestService extends RestServiceBase {

	public static final String INDEXER_TYPE_ES_RIVER_REMOTE = "elasticsearch-river-remote";
	public static final String INDEXER_TYPE_ES_RIVER_JIRA = "elasticsearch-river-jira";

	@Inject
	protected ProviderService providerService;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Inject
	protected EsRiverRemoteIndexerHandler esRiverRemoteIndexerHandler;

	@Inject
	protected EsRiverJiraIndexerHandler esRiverJiraIndexerHandler;

	@Context
	protected SecurityContext securityContext;

	/**
	 * Force reindex for given content type using Searchisko internal indexer.
	 * 
	 * @throws ObjectNotFoundException
	 */
	@POST
	@Path("/_force_reindex")
	@ProviderAllowed
	public Response forceReindex(@PathParam("type") String type) throws ObjectNotFoundException {

		Map<String, Object> ic = getIndexerConfigurationWithManagePermissionCheck(type);

		IndexerHandler ih = getIndexerHandler((String) ic.get(ProviderService.TYPE), type);

		try {
			ih.forceReindex(extractIndexerName(ic, type));
			return Response.ok("Full reindex forced successfuly").build();
		} catch (ObjectNotFoundException e) {
			throw new ObjectNotFoundException(
					"Indexer name or type is not configured correctly because indexer instance has not found for content type "
							+ type);
		}
	}

	/**
	 * Get status information of indexer for given content type using Searchisko internal indexer.
	 * 
	 * @throws ObjectNotFoundException
	 */
	@GET
	@Path("/_status")
	@ProviderAllowed
	public Object getStatus(@PathParam("type") String type) throws ObjectNotFoundException {

		Map<String, Object> ic = getIndexerConfigurationWithManagePermissionCheck(type);

		IndexerHandler ih = getIndexerHandler((String) ic.get(ProviderService.TYPE), type);

		try {
			return ih.getStatus(extractIndexerName(ic, type));
		} catch (ObjectNotFoundException e) {
			throw new ObjectNotFoundException(
					"Indexer name or type is not configured correctly because indexer instance has not found for content type "
							+ type);
		}
	}

	/**
	 * @param contentType of content we work for - used for error messages
	 * @param ic indexer configuration to extract from
	 * @return
	 * @throws ObjectNotFoundException
	 */
	protected String extractIndexerName(Map<String, Object> ic, String contentType) throws ObjectNotFoundException {
		String indexerName = SearchUtils.trimToNull((String) ic.get(ProviderService.NAME));
		if (indexerName == null) {
			throw new ObjectNotFoundException("Indexer name is not configured correctly for content type " + contentType);
		}
		return indexerName;
	}

	/**
	 * Get indexer configuration for given content type with validations and permission check.
	 * 
	 * @param contentType to get indexer configuration for.
	 * @return indexer configuration structure, never null.
	 * @throws ObjectNotFoundException if indexer configuration is not found for given content type
	 * @throws NotAuthorizedException if user is not authorized
	 * @throws NotAuthenticatedException if user is not authenticated
	 */
	// TODO #77 UNIT TEST
	protected Map<String, Object> getIndexerConfigurationWithManagePermissionCheck(String contentType)
			throws ObjectNotFoundException {
		if (contentType == null || SearchUtils.isBlank(contentType)) {
			throw new RequiredFieldException("type");
		}

		ProviderContentTypeInfo typeInfo = providerService.findContentType(contentType);
		if (typeInfo == null) {
			throw new BadFieldException("type", "content type not found");
		}
		authenticationUtilService.checkProviderManagementPermission(securityContext, typeInfo.getProviderName());

		Map<String, Object> ic = ProviderService.extractIndexerConfiguration(typeInfo.getTypeDef(), contentType);
		if (ic == null) {
			throw new ObjectNotFoundException("Indexer is not configured for content type " + contentType);
		}
		return ic;
	}

	/**
	 * Get indexer handler based on indexer type.
	 * 
	 * @param indexerType to get handler for
	 * @param contentType we handle indexer for - used for error messages only
	 * @return indexer handler, never null.
	 * @throws ObjectNotFoundException if indexer handler for passed in type doesn't exist.
	 */
	protected IndexerHandler getIndexerHandler(String indexerType, String contentType) throws ObjectNotFoundException {
		indexerType = SearchUtils.trimToNull(indexerType);
		if (indexerType == null) {
			throw new ObjectNotFoundException("Indexer type is not configured correctly for content type " + contentType);
		}

		if (INDEXER_TYPE_ES_RIVER_REMOTE.equals(indexerType)) {
			return esRiverRemoteIndexerHandler;
		} else if (INDEXER_TYPE_ES_RIVER_JIRA.equals(indexerType)) {
			return esRiverJiraIndexerHandler;
		} else {
			throw new ObjectNotFoundException("Unsupported indexer type '" + indexerType + "' configured for content type "
					+ contentType);
		}
	}

}
