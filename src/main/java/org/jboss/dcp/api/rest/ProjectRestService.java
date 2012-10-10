/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.annotations.security.ProviderAllowed;
import org.jboss.dcp.api.service.EntityService;

/**
 * Project REST API
 * 
 * @author Libor Krzyzanek
 * 
 */
@RequestScoped
@Path("/project")
@ProviderAllowed(superProviderOnly = true)
public class ProjectRestService extends RestEntityServiceBase {

	@Inject
	@Named("projectService")
	private EntityService entityService;

	@PostConstruct
	public void init() {
		setEntityService(entityService);
	}

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	@GuestAllowed
	public Object getAll(@QueryParam("from") Integer from, @QueryParam("size") Integer size) {
		return super.getAll(from, size);
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	@GuestAllowed
	public Object get(@PathParam("id") String id) {
		return super.get(id);
	}

}
