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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.search.SearchResponse;
import org.searchisko.api.annotations.header.CORSSupport;
import org.searchisko.api.annotations.security.GuestAllowed;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.service.ContributorService;

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

    /*
    // Commented out as there should be better implementation of it
    // as a part of https://github.com/searchisko/searchisko/issues/5
    @GET
    @Path("/")
    @Override
    @GuestAllowed
    @CORSSupport
    @Produces(MediaType.APPLICATION_JSON)
    public Object getAll(@QueryParam("from") Integer from, @QueryParam("size") Integer size) {
        return super.getAll(from, size);
    }

    @GET
    @Path("/{id}")
    @Override
    @GuestAllowed
    @CORSSupport
    @Produces(MediaType.APPLICATION_JSON)
    public Object get(@PathParam("id") String id) {
        return super.get(id);
    }
    */
}
