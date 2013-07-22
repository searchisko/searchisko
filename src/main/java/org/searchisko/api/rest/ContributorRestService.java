/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.elasticsearch.action.search.SearchResponse;
import org.searchisko.api.annotations.header.CORSSupport;
import org.searchisko.api.annotations.security.GuestAllowed;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.service.ContributorService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Contributor REST API
 * 
 * @author Libor Krzyzanek
 * 
 */
@RequestScoped
@Path("/contributor")
@ProviderAllowed(superProviderOnly = true)
public class ContributorRestService extends RestEntityServiceBase {

	@Inject
	protected ContributorService contributorService;

	@PostConstruct
	public void init() {
		setEntityService(contributorService);
	}

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
    @CORSSupport
	public Object search(@QueryParam("email") String email) {
		try {
			SearchResponse response = contributorService.search(email);
			return new ESDataOnlyResponse(response);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

}
