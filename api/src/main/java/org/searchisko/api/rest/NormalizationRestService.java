/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.common.settings.SettingsException;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorFactory;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ConfigService;
import org.searchisko.api.service.SearchClientService;
import org.searchisko.api.util.PreprocessChainContextImpl;
import org.searchisko.api.util.SearchUtils;

/**
 * REST API for Normalization.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/normalization")
@RolesAllowed({Role.ADMIN, Role.PROVIDER})
public class NormalizationRestService extends RestServiceBase {

	public static final String INKEY_ID = "id";

	/** Configuration Key for preprocessors setting **/
	public static final String CFG_PREPROCESSORS = "preprocessors";
	/** Configuration Key for preprocessors setting **/
	public static final String CFG_DESCRIPTION = "description";

	/** Normalization output structure key */
	public static final String OUTKEY_WARNINGS = "warnings";
	/** Normalization output structure key */
	public static final String OUTKEY_INPUT_ID = "input_id";

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected ConfigService configService;

	/**
	 * Get names and descriptions of all available normalizations.
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> availableNormalizations() {

		Map<String, Object> normalizations = configService.get(ConfigService.CFGNAME_NORMALIZATIONS);

		Map<String, Object> ret = new HashMap<>();

		if (normalizations != null) {
			for (String normalizationName : normalizations.keySet()) {
				Object o = normalizations.get(normalizationName);
				if (o instanceof Map) {
					String descr = ((Map<String, String>) o).get(CFG_DESCRIPTION);
					if (descr == null)
						descr = "";
					ret.put(normalizationName, descr);
				}
			}
		}
		return ret;
	}

	/**
	 * Run normalization of defined type for one input id
	 */
	@GET
	@Path("/{normalizationName}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object normalizeOne(@PathParam("normalizationName") String normalizationName, @PathParam(INKEY_ID) String id)
			throws ObjectNotFoundException {

		if (SearchUtils.isBlank(normalizationName)) {
			throw new RequiredFieldException("normalizationName");
		}

		if (SearchUtils.isBlank(id)) {
			throw new RequiredFieldException(INKEY_ID);
		}

		List<StructuredContentPreprocessor> preprocessors = getPreprocessors(normalizationName);

		return runPreprocessors(preprocessors, id);
	}

	/**
	 * Run normalization of defined type for more input id's
	 */
	@GET
	@Path("/{normalizationName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object normalizeBulk(@PathParam("normalizationName") String normalizationName, @Context UriInfo uriInfo)
			throws ObjectNotFoundException {
		if (SearchUtils.isBlank(normalizationName)) {
			throw new RequiredFieldException("normalizationName");
		}

		if (uriInfo == null || uriInfo.getQueryParameters().isEmpty()
				|| !uriInfo.getQueryParameters().containsKey(INKEY_ID)) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Request content must contain 'id' field with array of strings with content identifiers to delete")
					.build();
		}

		List<String> ids = uriInfo.getQueryParameters().get(INKEY_ID);

		Map<String, Object> ret = new LinkedHashMap<>();

		if (!ids.isEmpty()) {
			List<StructuredContentPreprocessor> preprocessors = getPreprocessors(normalizationName);
			for (String id : ids) {
				ret.put(id, runPreprocessors(preprocessors, id));
			}
		}
		return ret;
	}

	/**
	 * Get preprocessors for given normalization name.
	 * 
	 * @param normalizationName we want preprocessors for
	 * @return list of preprocessors
	 * @throws SettingsException if configuration is incorrect
	 * @throws ObjectNotFoundException if normalization of given name is not found
	 */
	@SuppressWarnings("unchecked")
	public List<StructuredContentPreprocessor> getPreprocessors(String normalizationName) throws SettingsException,
			ObjectNotFoundException {

		Map<String, Object> normalizations = configService.get(ConfigService.CFGNAME_NORMALIZATIONS);

		if (normalizations == null || normalizations.isEmpty() || !normalizations.containsKey(normalizationName)) {
			throw new ObjectNotFoundException("Normalization '" + normalizationName + "' is not found in configuration.");
		}

		Object o = normalizations.get(normalizationName);

		if (!(o instanceof Map)) {
			throw new ObjectNotFoundException("Normalization '" + normalizationName + "' is not configured properly.");
		}

		Map<String, Object> normalizationDef = (Map<String, Object>) o;

		try {
			return StructuredContentPreprocessorFactory.createPreprocessors(
					extractPreprocessors(normalizationDef, normalizationName), searchClientService.getClient());
		} catch (IllegalArgumentException | ClassCastException e) {
			throw new SettingsException("Bad configuration for normalization '" + normalizationName + "'. Cause: "
					+ e.getMessage(), e);
		}

	}

	/**
	 * Run defined content preprocessors on passed in content.
	 * 
	 * @param preprocessors to run
	 * @param content to run preprocessors on
	 * @return list of warnings from preprocessors, may be null
	 * @throws SettingsException if configuration is incorrect
	 */
	public Map<String, Object> runPreprocessors(List<StructuredContentPreprocessor> preprocessors, String id)
			throws SettingsException {

		Map<String, Object> content = new HashMap<>();
		content.put(OUTKEY_INPUT_ID, id);

		if (preprocessors != null && !preprocessors.isEmpty()) {

			PreprocessChainContextImpl context = new PreprocessChainContextImpl();
			for (StructuredContentPreprocessor preprocessor : preprocessors) {
				content = preprocessor.preprocessData(content, context);
			}

			if (context.warnings != null && !context.warnings.isEmpty())
				content.put(OUTKEY_WARNINGS, context.warnings);
		}
		return content;
	}

	/**
	 * Get preprocessors configuration from one normalization configuration structure.
	 * 
	 * @param normalizationDef normalization configuration structure
	 * @param normalizationName normalization name to be used for error messages
	 * @return list of preprocessor configurations
	 */
	@SuppressWarnings("unchecked")
	protected static List<Map<String, Object>> extractPreprocessors(Map<String, Object> normalizationDef,
			String normalizationName) {
		try {
			return (List<Map<String, Object>>) normalizationDef.get(CFG_PREPROCESSORS);
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration of 'preprocessors' for normalization '" + normalizationName
					+ "'.");
		}
	}

}
