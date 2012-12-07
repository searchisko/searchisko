/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.security.Principal;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

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

	protected final String[] fieldsToRemove = new String[] { "type_specific_code" };

	@Inject
	@Named("projectService")
	protected EntityService projectService;

	@Context
	protected SecurityContext securityContext;

	@PostConstruct
	public void init() {
		setEntityService(projectService);
	}

	/**
	 * Get all projects. If user is authenticated then all data are returned otherwise sensitive data are removed.
	 * 
	 * @see #fieldsToRemove
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	@GuestAllowed
	public Object getAll(@QueryParam("from") Integer from, @QueryParam("size") Integer size) {
		Principal principal = securityContext.getUserPrincipal();

		try {
			if (principal == null) {
				return entityService.getAll(from, size, fieldsToRemove);
			} else {
				return entityService.getAll(from, size, null);
			}
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	@GuestAllowed
	public Object get(@PathParam("id") String id) {
		Principal principal = securityContext.getUserPrincipal();
		// TODO: Security: At the moment principal is always null because it's GuestAllowed annotation
		// It's needed to improve authentication handling and store principal always not only on distinct operations
		if (principal == null) {
			return super.getFiltered(id, fieldsToRemove);
		} else {
			return super.get(id);
		}
	}

}
