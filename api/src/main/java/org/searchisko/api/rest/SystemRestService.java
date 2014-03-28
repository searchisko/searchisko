/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.searchisko.api.annotations.security.GuestAllowed;
import org.searchisko.api.annotations.security.ProviderAllowed;
import org.searchisko.api.rest.security.ProviderCustomSecurityContext;
import org.searchisko.api.service.SystemInfoService;

/**
 * System related REST service
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Path("/sys")
@RequestScoped
@ProviderAllowed
public class SystemRestService {

	@Context
	protected SecurityContext securityContext;

	@Inject
	private SystemInfoService systemInfoService;

	@GET
	@Path("/info")
	@Produces(MediaType.APPLICATION_JSON)
	@GuestAllowed
	public Map<Object, Object> info() throws IOException {

		return systemInfoService
				.getSystemInfo(securityContext.isUserInRole(ProviderCustomSecurityContext.SUPER_ADMIN_ROLE));
	}

}
