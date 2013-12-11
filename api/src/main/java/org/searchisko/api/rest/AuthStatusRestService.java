/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.searchisko.api.service.util.AuthenticationUtilService;

/**
 * Authentication status REST service.
 * 
 * @author Lukas Vlcek
 */
@Path("/auth")
@RequestScoped
public class AuthStatusRestService {

	@Inject
	private AuthenticationUtilService authenticationUtilService;

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> authStatus() {
		boolean authenticated = false;
		Map<String, Object> ret = new HashMap<>();
		String userName = authenticationUtilService.getAuthenticatedUserName();
		if (userName != null) {
			authenticated = true;
		}
		ret.put("authenticated", authenticated);
		return ret;
	}
}
