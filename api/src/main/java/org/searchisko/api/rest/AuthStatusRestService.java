/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.security.Role;

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

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	@RolesAllowed({Role.CONTRIBUTOR})
	public Map<String, Object> authStatus() {
		boolean authenticated = false;
		Map<String, Object> ret = new HashMap<>();
		try {
			authenticationUtilService.getAuthenticatedContributor(false);
			authenticated = true;
			authenticationUtilService.updateAuthenticatedContributorProfile();
		} catch (NotAuthenticatedException e) {
			log.log(Level.FINE, "Not authenticated.");
			// not authenticated so we return false
		}
		ret.put("authenticated", authenticated);
		return ret;
	}
}
