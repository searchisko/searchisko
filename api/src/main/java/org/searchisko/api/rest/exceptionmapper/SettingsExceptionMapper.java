/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.exceptionmapper;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.elasticsearch.common.settings.SettingsException;

/**
 * Mapper for the {@link SettingsException}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SettingsExceptionMapper implements ExceptionMapper<SettingsException> {

	@Inject
	protected Logger log;

	@Override
	public Response toResponse(SettingsException exception) {
		log.warning("Problem with Searchisko configuration: " + exception.getMessage());

		final Response.ResponseBuilder response = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
		response.entity("Problem with Searchisko configuration, contact Administrators please. Cause:"
				+ exception.getMessage());
		return response.build();
	}

}
