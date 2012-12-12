/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.jboss.dcp.api.annotations.security.ProviderAllowed;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.dcp.api.service.SecurityService;

/**
 * Provider REST API
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
@RequestScoped
@Path("/provider")
@ProviderAllowed(superProviderOnly = true)
public class ProviderRestService extends RestEntityServiceBase {

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SecurityService securityService;

	@Context
	protected SecurityContext securityContext;

	@PostConstruct
	public void init() {
		setEntityService(providerService.getEntityService());
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ProviderAllowed
	@Override
	public Object get(@PathParam("id") String id) {

		if (id == null || id.isEmpty()) {
			return createRequiredFieldResponse("id");
		}

		// check Provider name. Only provider with same name or superprovider has access.
		String provider = securityContext.getUserPrincipal().getName();
		try {
			Map<String, Object> entity = entityService.get(id);

			if (entity == null)
				return Response.status(Status.NOT_FOUND).build();

			if (!provider.equals(entity.get(ProviderService.NAME))) {
				if (!providerService.isSuperProvider(provider)) {
					return Response.status(Status.FORBIDDEN).build();
				}
			}
			return entity;
		} catch (Exception e) {
			return createErrorResponse(e);
		}
	}

	@POST
	@Path("/{id}/password")
	@ProviderAllowed
	public Object changePassword(@PathParam("id") String id, String pwd) {

		if (id == null || id.isEmpty()) {
			return createRequiredFieldResponse("id");
		}

		if (pwd == null || pwd.isEmpty()) {
			return createRequiredFieldResponse("pwd");
		}

		// check Provider name. Only provider with same name or superprovider has access.
		String provider = securityContext.getUserPrincipal().getName();

		Map<String, Object> entity = entityService.get(id);

		if (entity == null)
			return Response.status(Status.NOT_FOUND).build();

		String username = entity.get(ProviderService.NAME).toString();
		if (!provider.equals(username)) {
			if (!providerService.isSuperProvider(provider)) {
				return Response.status(Status.FORBIDDEN).build();
			}
		}
		entity.put(ProviderService.PASSWORD_HASH, securityService.createPwdHash(username, pwd));
		entityService.update(id, entity);

		return Response.ok().build();
	}
}
