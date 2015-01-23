/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import org.searchisko.api.audit.annotation.Audit;
import org.searchisko.api.audit.annotation.AuditContent;
import org.searchisko.api.audit.annotation.AuditId;
import org.searchisko.api.audit.annotation.AuditIgnore;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.RegisteredQueryService;
import org.searchisko.api.util.SearchUtils;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registered Query management REST API.
 *
 * @author Lukas Vlcek
 */
@RequestScoped
@Path("/query")
@RolesAllowed({Role.ADMIN})
@Audit
public class RegisteredQueryRestService extends RestEntityServiceBase {

	protected static final String[] FIELDS_TO_REMOVE = new String[] {
			RegisteredQueryService.FIELD_TEMPLATE,
			RegisteredQueryService.FIELD_OVERRIDE,
			RegisteredQueryService.FIELD_DEFAULT
	};

	@Inject
	@Named("registeredQueryService")
	protected RegisteredQueryService registeredQueryService;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@PostConstruct
	public void init() {
		setEntityService(registeredQueryService);
	}

	/**
	 * Get all registered queries. If user is authenticated then all data are returned otherwise sensitive data are removed.
	 *
	 * @see #FIELDS_TO_REMOVE
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	@PermitAll
	@AuditIgnore
	public Object getAll(@QueryParam("from") Integer from, @QueryParam("size") Integer size) {
		if (!authenticationUtilService.isUserInAnyOfRoles(true, Role.PROVIDER) ) {
			return entityService.getAll(from, size, FIELDS_TO_REMOVE);
		} else {
			return entityService.getAll(from, size, null);
		}
	}

	/**
	 * Get registered query having specified id.
	 *
	 * @param id
	 * @return
	 */
	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@Override
	@PermitAll
	@AuditIgnore
	public Object get(@PathParam("id") String id) {
		if (!authenticationUtilService.isUserInAnyOfRoles(true, Role.PROVIDER) ) {
			return super.getFiltered(id, FIELDS_TO_REMOVE);
		} else {
			return super.get(id);
		}
	}

	/**
	 * Create registered query. A query id must be present in the data.
	 *
	 * @param data
	 * @return
	 */
	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(@AuditContent Map<String, Object> data) {
		String nameFromData = (String) data.get(RegisteredQueryService.FIELD_NAME);
		if (nameFromData == null || nameFromData.isEmpty())
			return Response.status(Status.BAD_REQUEST)
					.entity("Required data field '" + RegisteredQueryService.FIELD_NAME + "' not set").build();
		return this.create(nameFromData, data);
	}

	/**
	 * Create or update registered query.
	 *
	 * @param id
	 * @param data
	 * @return
	 */
	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(@PathParam("id") @AuditId String id, @AuditContent Map<String, Object> data) {
		if (id == null || id.isEmpty()) {
			throw new RequiredFieldException("id");
		}

		Map<String, Object> dataWithSupportedFields = new HashMap<>();

		String nameFromData = (String) data.get(RegisteredQueryService.FIELD_NAME);
		if (nameFromData == null || nameFromData.isEmpty()) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Required data field '" + RegisteredQueryService.FIELD_NAME + "' not set").build();
		}

		if (!id.equals(nameFromData)) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Code in URL must be same as '" + RegisteredQueryService.FIELD_NAME + "' field in data.").build();
		}

		Object templateFromData = data.get(RegisteredQueryService.FIELD_TEMPLATE);
		if (templateFromData instanceof Map) {
			if (((Map)templateFromData).isEmpty()) {
				return Response.status(Status.BAD_REQUEST)
						.entity("Required data field '" + RegisteredQueryService.FIELD_TEMPLATE + "' not set").build();
			}
		} else if (templateFromData instanceof String) {
			if (((String)templateFromData).trim().isEmpty()) {
				return Response.status(Status.BAD_REQUEST)
						.entity("Required data field '" + RegisteredQueryService.FIELD_TEMPLATE + "' not set").build();
			}
		} else {
			return Response.status(Status.BAD_REQUEST)
					.entity("Required data field '" + RegisteredQueryService.FIELD_TEMPLATE + "' not set").build();
		}

		dataWithSupportedFields.put(RegisteredQueryService.FIELD_NAME, nameFromData);
		dataWithSupportedFields.put(RegisteredQueryService.FIELD_TEMPLATE, templateFromData);

		try {
			@SuppressWarnings("unchecked")
			List<String> rolesFromData = (List<String>) data.get(RegisteredQueryService.FIELD_ALLOWED_ROLES);
			if (rolesFromData == null || rolesFromData.isEmpty()) {
				log.info("Registered query does not specify any roles. It will be available to public.");
			} else {
				dataWithSupportedFields.put(RegisteredQueryService.FIELD_ALLOWED_ROLES, rolesFromData);
			}
		} catch (ClassCastException e) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Data field '" + RegisteredQueryService.FIELD_ALLOWED_ROLES + "' must be an array of string(s).").build();
		}

		String descriptionFromData = (String) data.get(RegisteredQueryService.FIELD_DESCRIPTION);
		if (descriptionFromData != null && !descriptionFromData.isEmpty()) {
			dataWithSupportedFields.put(RegisteredQueryService.FIELD_DESCRIPTION, descriptionFromData);
		}

		Object defaultFromData = data.get(RegisteredQueryService.FIELD_DEFAULT);
		if (defaultFromData instanceof Map) {
			dataWithSupportedFields.put(RegisteredQueryService.FIELD_DEFAULT, defaultFromData);
		}

		Object overrideFromData = data.get(RegisteredQueryService.FIELD_OVERRIDE);
		if (overrideFromData instanceof Map) {
			dataWithSupportedFields.put(RegisteredQueryService.FIELD_OVERRIDE, overrideFromData);
		}

		registeredQueryService.create(id, dataWithSupportedFields);
		return createResponseWithId(id);
	}

	/**
	 * Delete registered query.
	 *
	 * @param id
	 * @return
	 */
	@DELETE
	@Path("/{id}")
	public Object delete(@PathParam("id") @AuditId String id) {
		if ((id = SearchUtils.trimToNull(id)) == null) {
			throw new RequiredFieldException("id");
		}
		registeredQueryService.delete(id);
		return Response.ok().build();
	}
}
