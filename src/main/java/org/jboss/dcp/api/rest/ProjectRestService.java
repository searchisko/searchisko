/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import org.jboss.dcp.api.annotations.header.AccessControlAllowOrigin;
import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.annotations.security.ProviderAllowed;
import org.jboss.dcp.api.service.ProjectService;
import org.jboss.dcp.persistence.service.EntityService;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Map;

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
    @AccessControlAllowOrigin
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
    @AccessControlAllowOrigin
	public Object get(@PathParam("id") String id) {
		Principal principal = securityContext.getUserPrincipal();
		if (principal == null) {
			return super.getFiltered(id, fieldsToRemove);
		} else {
			return super.get(id);
		}
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(Map<String, Object> data) {
		String codeFromData = (String) data.get(ProjectService.CODE);
		if (codeFromData == null || codeFromData.isEmpty())
			return Response.status(Status.BAD_REQUEST).entity("Required data field '" + ProjectService.CODE + "' not set")
					.build();
		return this.create(codeFromData, data);
	}

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(@PathParam("id") String id, Map<String, Object> data) {

		if (id == null || id.isEmpty()) {
			return createRequiredFieldResponse("id");
		}

		String codeFromData = (String) data.get(ProjectService.CODE);
		if (codeFromData == null || codeFromData.isEmpty())
			return Response.status(Status.BAD_REQUEST).entity("Required data field '" + ProjectService.CODE + "' not set")
					.build();

		if (!id.equals(codeFromData)) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Code in URL must be same as '" + ProjectService.CODE + "' field in data.").build();
		}

		try {
			entityService.create(id, data);
			return createResponseWithId(id);
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

}
