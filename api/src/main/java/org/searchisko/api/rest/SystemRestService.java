/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.searchisko.api.security.Role;
import org.searchisko.api.service.SystemInfoService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Map;

/**
 * System related REST service
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Path("/sys")
@RequestScoped
@RolesAllowed(Role.PROVIDER)
public class SystemRestService {

	@Context
	protected SecurityContext securityContext;

	@Inject
	private SystemInfoService systemInfoService;

	@GET
	@Path("/info")
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	public Map<Object, Object> info() throws IOException {
		return systemInfoService.getSystemInfo(securityContext.isUserInRole(Role.ADMIN));
	}

}
