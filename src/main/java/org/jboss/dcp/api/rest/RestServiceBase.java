/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 * Base for REST endpoint services.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
public class RestServiceBase {

	@Inject
	protected Logger log;

	@Context
	protected SecurityContext securityContext;

	/**
	 * Get provider name based on security user principal
	 * 
	 * @return
	 */
	public String getProvider() {
		return securityContext.getUserPrincipal().getName();
	}

	/**
	 * Create response based on elastic get response
	 * 
	 * @param response
	 * @return
	 * @throws IOException
	 */
	public Map<String, Object> createResponse(final GetResponse response) {
		return response.getSource();
	}

	/**
	 * Create response based on elastic search response
	 * 
	 * @param response elastic search response to return
	 * @param responseUuid UUID of response
	 * @return
	 * @throws IOException
	 */
	public StreamingOutput createResponse(final SearchResponse response, final String responseUuid) {
		return new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				XContentBuilder builder = XContentFactory.jsonBuilder(output);
				builder.startObject();
				if (responseUuid != null)
					builder.field("uuid", responseUuid);
				response.toXContent(builder, ToXContent.EMPTY_PARAMS);
				builder.endObject();
				builder.close();
			}
		};
	}

	public Response createRequiredFieldResponse(String fieldName) {
		log.log(Level.FINE, "Required parameter {0} not set", fieldName);
		return Response.status(Status.BAD_REQUEST).entity("Required parameter '" + fieldName + "' not set").build();
	}

	public Response createBadFieldDataResponse(String fieldName) {
		log.log(Level.FINE, "Parameter {0} has bad value", fieldName);
		return Response.status(Status.BAD_REQUEST).entity("Parameter '" + fieldName + "' has bad value").build();
	}

	public Response createErrorResponse(Exception ex) {
		if (log.isLoggable(Level.WARNING)) {
			log.log(Level.WARNING, "Exception {0} occurred. Message: {1}",
					new Object[] { ex.getClass().getName(), ex.getMessage() });
			log.log(Level.FINE, "Exception trace.", ex);
		}
		if (ex instanceof IllegalArgumentException) {
			return Response.serverError().entity(ex.getMessage()).build();
		}
		return Response.serverError().entity("Error [" + ex.getClass().getName() + "]: " + ex.getMessage()).build();
	}

}
