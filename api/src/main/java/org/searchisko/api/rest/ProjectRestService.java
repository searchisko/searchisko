/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.elasticsearch.action.search.SearchResponse;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ProjectService;
import org.searchisko.api.util.SearchUtils;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.security.Principal;
import java.util.Map;

/**
 * Project REST API
 *
 * @author Libor Krzyzanek
 */
@RequestScoped
@Path("/project")
@RolesAllowed(Role.ADMIN)
public class ProjectRestService extends RestEntityServiceBase {

	protected final String[] fieldsToRemove = new String[]{"type_specific_code"};

	public static final String PARAM_CODE = "code";

	@Inject
	@Named("projectService")
	protected ProjectService projectService;

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
	@PermitAll
	public Object getAll(@QueryParam("from") Integer from, @QueryParam("size") Integer size) {
		Principal principal = securityContext.getUserPrincipal();

		if (principal == null) {
			return entityService.getAll(from, size, fieldsToRemove);
		} else {
			return entityService.getAll(from, size, null);
		}
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	@PermitAll
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
		String codeFromData = (String) data.get(ProjectService.FIELD_CODE);
		if (codeFromData == null || codeFromData.isEmpty())
			return Response.status(Status.BAD_REQUEST)
					.entity("Required data field '" + ProjectService.FIELD_CODE + "' not set").build();
		return this.create(codeFromData, data);
	}

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(@PathParam("id") String id, Map<String, Object> data) {

		if (id == null || id.isEmpty()) {
			throw new RequiredFieldException("id");
		}

		String codeFromData = (String) data.get(ProjectService.FIELD_CODE);
		if (codeFromData == null || codeFromData.isEmpty())
			return Response.status(Status.BAD_REQUEST)
					.entity("Required data field '" + ProjectService.FIELD_CODE + "' not set").build();

		if (!id.equals(codeFromData)) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Code in URL must be same as '" + ProjectService.FIELD_CODE + "' field in data.").build();
		}

		entityService.create(id, data);
		return createResponseWithId(id);
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
			response = projectService.findByCode(codeValue);
		} else {
			response = projectService.findByTypeSpecificCode(codeName, codeValue);
		}

		return new ESDataOnlyResponse(response);
	}

}
