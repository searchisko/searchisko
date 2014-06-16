/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
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

import org.searchisko.api.audit.annotation.Audit;
import org.searchisko.api.audit.annotation.AuditId;
import org.searchisko.api.audit.annotation.AuditIgnore;
import org.searchisko.api.indexer.EsRiverJiraIndexerHandler;
import org.searchisko.api.indexer.EsRiverRemoteIndexerHandler;
import org.searchisko.api.indexer.IndexerHandler;
import org.searchisko.api.rest.exception.BadFieldException;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.ProviderService.ProviderContentTypeInfo;
import org.searchisko.api.util.SearchUtils;

/**
 * REST API for Indexer related operations.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/indexer")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({ Role.ADMIN, Role.PROVIDER })
@Audit
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
	@Path("/{type}/_force_reindex")
	public Response forceReindex(@PathParam("type") @AuditId String type) throws ObjectNotFoundException {

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
	 * Stop indexing for all content types using Searchisko internal indexer.
	 * 
	 * @throws ObjectNotFoundException
	 */
	@POST
	@Path("/_all/_stop")
	public Map<String, String> stopAll() throws ObjectNotFoundException {

		Map<String, String> ret = new HashMap<>();

		List<ProviderContentTypeInfo> allI = getAllIndexerConfigurationsWithManagePermissionCheck();

		for (ProviderContentTypeInfo typeInfo : allI) {
			String typeName = typeInfo.getTypeName();
			Map<String, Object> ic = ProviderService.extractIndexerConfiguration(typeInfo.getTypeDef(), typeName);

			IndexerHandler ih = getIndexerHandler((String) ic.get(ProviderService.TYPE), typeName);

			try {
				ih.stop(extractIndexerName(ic, typeName));
				ret.put(typeName, "Indexer stopped successfuly");
			} catch (ObjectNotFoundException e) {
				ret.put(typeName, "Indexer name or type is not configured correctly because indexer instance has not found");
			}
		}

		return ret;
	}

	/**
	 * Stop indexing for given content type using Searchisko internal indexer.
	 * 
	 * @throws ObjectNotFoundException
	 */
	@POST
	@Path("/{type}/_stop")
	public Response stop(@PathParam("type") @AuditId String type) throws ObjectNotFoundException {

		Map<String, Object> ic = getIndexerConfigurationWithManagePermissionCheck(type);

		IndexerHandler ih = getIndexerHandler((String) ic.get(ProviderService.TYPE), type);

		try {
			ih.stop(extractIndexerName(ic, type));
			return Response.ok("Indexer stopped successfuly").build();
		} catch (ObjectNotFoundException e) {
			throw new ObjectNotFoundException(
					"Indexer name or type is not configured correctly because indexer instance has not found for content type "
							+ type);
		}
	}

	/**
	 * Restart indexing for all content types using Searchisko internal indexer.
	 * 
	 * @throws ObjectNotFoundException
	 */
	@POST
	@Path("/_all/_restart")
	public Map<String, String> restartAll() throws ObjectNotFoundException {

		Map<String, String> ret = new HashMap<>();

		List<ProviderContentTypeInfo> allI = getAllIndexerConfigurationsWithManagePermissionCheck();

		for (ProviderContentTypeInfo typeInfo : allI) {
			String typeName = typeInfo.getTypeName();
			Map<String, Object> ic = ProviderService.extractIndexerConfiguration(typeInfo.getTypeDef(), typeName);

			IndexerHandler ih = getIndexerHandler((String) ic.get(ProviderService.TYPE), typeName);

			try {
				ih.restart(extractIndexerName(ic, typeName));
				ret.put(typeName, "Indexer restarted successfuly");
			} catch (ObjectNotFoundException e) {
				ret.put(typeName, "Indexer name or type is not configured correctly because indexer instance has not found");
			}
		}

		return ret;
	}

	/**
	 * Restart indexing for given content type using Searchisko internal indexer.
	 * 
	 * @throws ObjectNotFoundException
	 */
	@POST
	@Path("/{type}/_restart")
	public Response restart(@PathParam("type") @AuditId String type) throws ObjectNotFoundException {

		Map<String, Object> ic = getIndexerConfigurationWithManagePermissionCheck(type);

		IndexerHandler ih = getIndexerHandler((String) ic.get(ProviderService.TYPE), type);

		try {
			ih.restart(extractIndexerName(ic, type));
			return Response.ok("Indexer restarted successfuly").build();
		} catch (ObjectNotFoundException e) {
			throw new ObjectNotFoundException(
					"Indexer name or type is not configured correctly because indexer instance has not found for content type "
							+ type);
		}
	}

	/**
	 * Get status information for all Searchisko internal indexer you have permission to manage.
	 * 
	 * @throws ObjectNotFoundException
	 */
	@GET
	@Path("/_all/_status")
	@AuditIgnore
	public Map<String, Object> statusAll() throws ObjectNotFoundException {

		Map<String, Object> ret = new HashMap<>();

		List<ProviderContentTypeInfo> allI = getAllIndexerConfigurationsWithManagePermissionCheck();

		for (ProviderContentTypeInfo typeInfo : allI) {
			String typeName = typeInfo.getTypeName();
			Map<String, Object> ic = ProviderService.extractIndexerConfiguration(typeInfo.getTypeDef(), typeName);

			IndexerHandler ih = getIndexerHandler((String) ic.get(ProviderService.TYPE), typeName);

			try {
				ret.put(typeName, ih.getStatus(extractIndexerName(ic, typeName)));
			} catch (ObjectNotFoundException e) {
				ret.put(typeName, "Indexer name or type is not configured correctly because indexer instance has not found");
			}
		}

		return ret;
	}

	/**
	 * Get status information of indexer for given content type using Searchisko internal indexer.
	 * 
	 * @throws ObjectNotFoundException
	 */
	@GET
	@Path("/{type}/_status")
	@AuditIgnore
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
	 * @see AuthenticationUtilService#checkProviderManagementPermission(String)
	 */
	protected Map<String, Object> getIndexerConfigurationWithManagePermissionCheck(String contentType)
			throws ObjectNotFoundException {
		if (contentType == null || SearchUtils.isBlank(contentType)) {
			throw new RequiredFieldException("type");
		}

		ProviderContentTypeInfo typeInfo = providerService.findContentType(contentType);
		if (typeInfo == null) {
			throw new BadFieldException("type", "content type not found");
		}
		authenticationUtilService.checkProviderManagementPermission(typeInfo.getProviderName());

		Map<String, Object> ic = ProviderService.extractIndexerConfiguration(typeInfo.getTypeDef(), contentType);
		if (ic == null) {
			throw new ObjectNotFoundException("Indexer is not configured for content type " + contentType);
		}
		return ic;
	}

	/**
	 * Get configuration of all indexers from all content types user has permission to manage (provider permission check).
	 * 
	 * @return list of type infos with indexers, never null.
	 * @see AuthenticationUtilService#checkProviderManagementPermission(String)
	 */
	protected List<ProviderContentTypeInfo> getAllIndexerConfigurationsWithManagePermissionCheck() {
		List<ProviderContentTypeInfo> ret = new ArrayList<>();

		List<Map<String, Object>> allProviders = providerService.getAll();

		if (allProviders != null) {
			for (Map<String, Object> providerDef : allProviders) {
				String providerName = (String) providerDef.get(ProviderService.NAME);
				try {
					authenticationUtilService.checkProviderManagementPermission(providerName);

					Map<String, Map<String, Object>> allCt = ProviderService.extractAllContentTypes(providerDef);
					if (allCt != null) {
						for (String typeName : allCt.keySet()) {
							Map<String, Object> typeDef = allCt.get(typeName);

							Map<String, Object> ic = ProviderService.extractIndexerConfiguration(typeDef, typeName);
							if (ic != null) {
								ret.add(new ProviderContentTypeInfo(providerDef, typeName));
							}
						}
					}
				} catch (NotAuthorizedException e) {
					// OK, ignore it
				}
			}
		}
		return ret;
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
