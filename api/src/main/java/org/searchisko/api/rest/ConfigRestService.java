/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import org.searchisko.api.audit.annotation.Audit;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ConfigService;

/**
 * Config REST API
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
@RequestScoped
@Path("/config")
@RolesAllowed(Role.ADMIN)
@Audit
public class ConfigRestService extends RestEntityServiceBase {

	@Inject
	protected ConfigService configService;

	@PostConstruct
	public void init() {
		setEntityService(configService);
	}

}
