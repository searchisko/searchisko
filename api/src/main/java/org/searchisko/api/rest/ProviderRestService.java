/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.rest.security.ProviderCustomSecurityContext;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SecurityService;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;

/**
 * Provider REST API
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/provider")
@RolesAllowed(Role.ADMIN)
public class ProviderRestService extends RestEntityServiceBase {

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SecurityService securityService;

	@Context
	protected SecurityContext securityContext;

	@PostConstruct
	public void init() {
		setEntityService(providerService);
	}

	protected static final String[] FIELDS_TO_REMOVE = new String[]{ProviderService.PASSWORD_HASH};

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getAll(@QueryParam("from") Integer from, @QueryParam("size") Integer size) {
		return entityService.getAll(from, size, FIELDS_TO_REMOVE);
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({Role.ADMIN, Role.PROVIDER})
	@Override
	public Object get(@PathParam("id") String id) {

		if (id == null || id.isEmpty()) {
			throw new RequiredFieldException("id");
		}

		// check Provider name. Only provider with same name or superprovider has access.
		String provider = securityContext.getUserPrincipal().getName();

		Map<String, Object> entity = entityService.get(id);

		if (entity == null)
			return Response.status(Status.NOT_FOUND).build();

		if (!(provider.equals(entity.get(ProviderService.NAME)) || securityContext
				.isUserInRole(ProviderCustomSecurityContext.SUPER_ADMIN_ROLE))) {
			return Response.status(Status.FORBIDDEN).build();
		}
		return ESDataOnlyResponse.removeFields(entity, FIELDS_TO_REMOVE);
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(Map<String, Object> data) {
		String nameFromData = (String) data.get(ProviderService.NAME);
		if (nameFromData == null || nameFromData.isEmpty())
			return Response.status(Status.BAD_REQUEST).entity("Required data field '" + ProviderService.NAME + "' not set")
					.build();
		return this.create(nameFromData, data);
	}

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(@PathParam("id") String id, Map<String, Object> data) {

		if (id == null || id.isEmpty()) {
			throw new RequiredFieldException("id");
		}

		String nameFromData = (String) data.get(ProviderService.NAME);
		if (nameFromData == null || nameFromData.isEmpty())
			return Response.status(Status.BAD_REQUEST).entity("Required data field '" + ProviderService.NAME + "' not set")
					.build();

		if (!id.equals(nameFromData)) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Name in URL must be same as '" + ProviderService.NAME + "' field in data.").build();
		}

		// do not update password hash if entity exists already!
		Map<String, Object> entity = providerService.get(id);
		if (entity != null) {
			Object pwdhash = entity.get(ProviderService.PASSWORD_HASH);
			if (pwdhash != null)
				data.put(ProviderService.PASSWORD_HASH, pwdhash);
		}
		providerService.create(id, data);
		return createResponseWithId(id);
	}

	@POST
	@Path("/{id}/password")
	@RolesAllowed({Role.ADMIN, Role.PROVIDER})
	public Object changePassword(@PathParam("id") String id, String pwd) {

		if (id == null || id.isEmpty()) {
			throw new RequiredFieldException("id");
		}

		if (pwd == null || pwd.trim().isEmpty()) {
			throw new RequiredFieldException("pwd");
		}

		// check Provider name. Only provider with same name or superprovider has access.
		String provider = securityContext.getUserPrincipal().getName();

		Map<String, Object> entity = entityService.get(id);

		if (entity == null)
			return Response.status(Status.NOT_FOUND).build();

		String username = entity.get(ProviderService.NAME).toString();
		if (!(provider.equals(entity.get(ProviderService.NAME)) || securityContext
				.isUserInRole(ProviderCustomSecurityContext.SUPER_ADMIN_ROLE))) {
			return Response.status(Status.FORBIDDEN).build();
		}
		entity.put(ProviderService.PASSWORD_HASH, securityService.createPwdHash(username, pwd.trim()));
		entityService.update(id, entity);

		return Response.ok().build();
	}

}
