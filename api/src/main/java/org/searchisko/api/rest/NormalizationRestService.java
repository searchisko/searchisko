/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.common.settings.SettingsException;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorFactory;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.rest.exception.RequiredFieldException;
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
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ProviderAllowed
public class NormalizationRestService extends RestServiceBase {

	@Inject
	protected SearchClientService searchClientService;

	@Inject
	protected ConfigService configService;

	/**
	 * Run normalization of defined type for one input id
	 */
	@SuppressWarnings("unchecked")
	@GET
	@Path("/{type}/{id}")
	@ProviderAllowed
	public Object normalizeOne(@PathParam("type") String type, @PathParam("id") String id) throws ObjectNotFoundException {

		if (type == null || SearchUtils.isBlank(type)) {
			throw new RequiredFieldException("type");
		}

		if (id == null || SearchUtils.isBlank(id)) {
			throw new RequiredFieldException("id");
		}

		Map<String, Object> normalizations = configService.get(ConfigService.CFGNAME_NORMALIZATIONS);

		if (normalizations == null || normalizations.isEmpty() || !normalizations.containsKey(type)) {
			throw new ObjectNotFoundException("Normalization '" + type + "' is not configured");
		}

		Object o = normalizations.get(type);

		if (!(o instanceof Map)) {
			throw new ObjectNotFoundException("Normalization '" + type + "' is not configured properly");
		}

		Map<String, Object> normalizationDef = (Map<String, Object>) o;

		Map<String, Object> content = new HashMap<>();
		content.put("input_id", id);
		List<Map<String, String>> warn = runPreprocessors(type, normalizationDef, content);

		if (warn != null && !warn.isEmpty())
			content.put("warnings", warn);

		return content;
	}

	// TODO #90 bulk version of the API operation

	// TODO #90 unit tests

	// TODO #90 documentation

	/**
	 * Run defined content preprocessors on passed in content.
	 * 
	 * @param normalizationName <code>sys_content_type</code> name we run preprocessors for to be used for error messages
	 * @param preprocessorsDef definition of preprocessors - see {@link #extractPreprocessors(Map, String)}
	 * @param content to run preprocessors on
	 * @return list of warnings from preprocessors, may be null
	 * @throws SettingsException if configuration is incorrect
	 */
	public List<Map<String, String>> runPreprocessors(String normalizationName, Map<String, Object> normalizationDef,
			Map<String, Object> content) throws SettingsException {
		List<StructuredContentPreprocessor> preprocessors = null;

		try {
			preprocessors = StructuredContentPreprocessorFactory.createPreprocessors(
					extractPreprocessors(normalizationDef, normalizationName), searchClientService.getClient());
		} catch (IllegalArgumentException | ClassCastException e) {
			throw new SettingsException("Bad configuration for normalization '" + normalizationName
					+ "'. Contact administrators please. Cause: " + e.getMessage(), e);
		}

		PreprocessChainContextImpl context = new PreprocessChainContextImpl();
		for (StructuredContentPreprocessor preprocessor : preprocessors) {
			content = preprocessor.preprocessData(content, context);
		}
		return context.warnings;
	}

	/** Configuration Key for preprocessors setting **/
	public static final String CFG_PREPROCESSORS = "preprocessors";

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
					+ "'. Contact administrators please.");
		}
	}

}
