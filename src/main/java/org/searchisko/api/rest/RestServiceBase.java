/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
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
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 * Base for REST endpoint services.
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukas Vlcek
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
	 * Create JAX-RS response based on elastic get response.
	 *
	 * @param response to process
	 * @return source from elastic search response
	 * @throws IOException
	 */
	public Map<String, Object> createResponse(final GetResponse response) {
		return response.getSource();
	}

	/**
	 * Create response structure with id field only.
	 *
	 * @param id value for id field
	 * @return response with id field
	 */
	protected Map<String, Object> createResponseWithId(String id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("id", id);
		return result;
	}

	/**
	 * Create JAX-RS response based on elastic search response
	 *
	 * @param response elastic search response to return
	 * @param additionalResponseFields map with additional fields added to the response root level object
	 * @return JAX-RS response
	 */
	public StreamingOutput createResponse(final SearchResponse response,
			final Map<String, String> additionalResponseFields) {
		return new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				XContentBuilder builder = XContentFactory.jsonBuilder(output);
				builder.startObject();
				if (additionalResponseFields != null) {
					for (String key : additionalResponseFields.keySet()) {
						builder.field(key, additionalResponseFields.get(key));
					}
				}
				response.toXContent(builder, ToXContent.EMPTY_PARAMS);
				builder.endObject();
				builder.close();
			}
		};
	}

	/**
	 * Create response based on elastic multi search response (does not add UUID for now)
	 *
	 * @param response
	 * @param responseUuid
	 * @return
	 */
	public StreamingOutput createResponse(final MultiSearchResponse response, final String responseUuid) {
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

    // TODO: All of these should be using JAX-RS exception mappers
	public Response createRequiredFieldResponse(String fieldName) {
		log.log(Level.FINE, "Required parameter {0} not set", fieldName);
		return Response.status(Status.BAD_REQUEST).entity("Required parameter '" + fieldName + "' not set").build();
	}

	public Response createBadFieldDataResponse(String fieldName) {
		log.log(Level.FINE, "Parameter {0} has bad value", fieldName);
		return Response.status(Status.BAD_REQUEST).entity("Parameter '" + fieldName + "' has bad value").build();
	}

	public Response createErrorResponse(Exception ex) {
		if (ex instanceof NullPointerException) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
		} else if (ex instanceof IllegalArgumentException) {
			// we use this exception in case of bad input parameters in request etc, message contains erro description
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, ex.getMessage(), ex);
			}
			return Response.serverError().entity(ex.getMessage()).build();
		} else {
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, "Exception {0} occurred. Message: {1}",
						new Object[] { ex.getClass().getName(), ex.getMessage() });
				log.log(Level.FINE, "Exception trace.", ex);
			}
		}
        // TODO: We need to find a way to do this per project stage. If we're in production we CERTAINLY don't want
        // to be opening ourselves up to an exploit by providing a stack trace.
		return Response.serverError().entity("Error [" + ex.getClass().getName() + "]: " + ex.getMessage()).build();
	}
}
