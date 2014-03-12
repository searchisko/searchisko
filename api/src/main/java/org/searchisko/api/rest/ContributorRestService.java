/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchResponse;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.service.ContributorService;
import org.searchisko.api.util.SearchUtils;

/**
 * Contributor REST API
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/contributor")
@ProviderAllowed(superProviderOnly = true)
public class ContributorRestService extends RestEntityServiceBase {

	public static final String PARAM_EMAIL = "email";
	public static final String PARAM_CODE = "code";

	@Inject
	protected ContributorService contributorService;

	@PostConstruct
	public void init() {
		setEntityService(contributorService);
	}

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Object search(@Context UriInfo uriInfo) {

		if (uriInfo == null || uriInfo.getQueryParameters().isEmpty() || uriInfo.getQueryParameters().size() > 1) {
			return Response.status(Response.Status.BAD_REQUEST).entity("One request parameter is expected").build();
		}

		SearchResponse response = null;

		String codeName = uriInfo.getQueryParameters().keySet().iterator().next();
		String codeValue = uriInfo.getQueryParameters().getFirst(codeName);

		if (SearchUtils.isBlank(codeValue)) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Value for request parameter " + codeName + " must be provided").build();
		}

		if (PARAM_CODE.equals(codeName)) {
			response = contributorService.findByCode(codeValue);
		} else if (PARAM_EMAIL.equals(codeName)) {
			response = contributorService.findByEmail(codeValue);
		} else {
			response = contributorService.findByTypeSpecificCode(codeName, codeValue);
		}

		return new ESDataOnlyResponse(response);
	}
}
