/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.rest.security.AuthenticationUtilService;
import org.searchisko.api.security.Role;

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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication status REST service.
 * 
 * @author Lukas Vlcek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Path("/auth")
@RequestScoped
public class AuthStatusRestService {

	@Inject
	protected Logger log;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Context
	protected SecurityContext securityContext;

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	@RolesAllowed({Role.CONTRIBUTOR})
	public Map<String, Object> authStatus() {
		log.log(Level.FINEST, "Security Context: {0}", securityContext);

		boolean authenticated = false;
		Map<String, Object> ret = new HashMap<>();
		try {
			authenticationUtilService.getAuthenticatedContributor(securityContext, false);
			authenticated = true;
			authenticationUtilService.updateAuthenticatedContributorProfile(securityContext);
		} catch (NotAuthenticatedException e) {
			log.log(Level.FINE, "Not authenticated.");
			// not authenticated so we return false
		}
		ret.put("authenticated", authenticated);
		return ret;
	}
}
