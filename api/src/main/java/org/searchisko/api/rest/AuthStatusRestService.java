/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.searchisko.api.rest.exception.NotAuthenticatedException;
import org.searchisko.api.service.AuthenticationUtilService;

/**
 * Authentication status REST service.
 * 
 * @author Lukas Vlcek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Path("/auth")
@RequestScoped
public class AuthStatusRestService {

	public static final String RESPONSE_FIELD_AUTHENTICATED = "authenticated";
	public static final String RESPONSE_FIELD_ROLES = "roles";

	@Inject
	protected Logger log;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	public Map<String, Object> authStatus(@QueryParam(RESPONSE_FIELD_ROLES) String rolesQp) {
		boolean authenticated = false;
		Map<String, Object> ret = new HashMap<>();
		try {
			authenticationUtilService.getAuthenticatedContributor(false);
			authenticated = true;
			authenticationUtilService.updateAuthenticatedContributorProfile();
			if (StringUtils.isNotBlank(rolesQp)) {
				Set<String> roles = authenticationUtilService.getUserRoles();
				if (roles != null && !roles.isEmpty())
					ret.put(RESPONSE_FIELD_ROLES, roles);
			}
		} catch (NotAuthenticatedException e) {
			log.log(Level.FINE, "Not authenticated.");
			// not authenticated so we return false
		}
		ret.put(RESPONSE_FIELD_AUTHENTICATED, authenticated);
		return ret;
	}
}
