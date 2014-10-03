/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.elasticsearch.action.search.SearchResponse;
import org.searchisko.api.audit.annotation.Audit;
import org.searchisko.api.audit.annotation.AuditContent;
import org.searchisko.api.audit.annotation.AuditId;
import org.searchisko.api.audit.annotation.AuditIgnore;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.ContributorService;
import org.searchisko.api.util.SearchUtils;

/**
 * Contributor REST API
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/contributor")
@RolesAllowed({Role.ADMIN, Role.CONTRIBUTORS_MANAGER})
@Audit
public class ContributorRestService extends RestEntityServiceBase {

	public static final String PARAM_EMAIL = "email";
	public static final String PARAM_CODE = "code";
	public static final String PARAM_NAME = "name";

	@Inject
	protected ContributorService contributorService;

	@PostConstruct
	public void init() {
		setEntityService(contributorService);
	}

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@AuditIgnore
	public Object search(@Context UriInfo uriInfo) {

		if (uriInfo == null || uriInfo.getQueryParameters().isEmpty() || uriInfo.getQueryParameters().size() > 1) {
			return Response.status(Response.Status.BAD_REQUEST).entity("One request parameter is expected").build();
		}

		SearchResponse response = null;

		String codeName = uriInfo.getQueryParameters().keySet().iterator().next();
		String codeValue = uriInfo.getQueryParameters().getFirst(codeName);

		if (SearchUtils.isBlank(codeValue)) {
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Value for request parameter " + codeName + " must be provided").build();
		}

		if (PARAM_CODE.equals(codeName)) {
			response = contributorService.findByCode(codeValue);
		} else if (PARAM_EMAIL.equals(codeName)) {
			response = contributorService.findByEmail(codeValue);
		} else if (PARAM_NAME.equals(codeName)) {
			response = contributorService.findByName(codeValue, false);
		} else {
			response = contributorService.findByTypeSpecificCode(codeName, codeValue);
		}

		return new ESDataOnlyResponse(response);
	}

	@POST
	@Path("/{id}/code/{code}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object codeChange(@PathParam("id") @AuditId String id, @AuditContent @PathParam("code") String code) throws ObjectNotFoundException {
		if ((id = SearchUtils.trimToNull(id)) == null) {
			throw new RequiredFieldException("id");
		}
		if ((code = SearchUtils.trimToNull(code)) == null) {
			throw new RequiredFieldException("code");
		}

		return contributorService.changeContributorCode(id, code);
	}

	@POST
	@Path("/{idFrom}/mergeTo/{idTo}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object mergeContributors(@PathParam("idFrom") @AuditId String idFrom, @AuditContent @PathParam("idTo") String idTo)
			throws ObjectNotFoundException {
		if ((idFrom = SearchUtils.trimToNull(idFrom)) == null) {
			throw new RequiredFieldException("idFrom");
		}
		if ((idTo = SearchUtils.trimToNull(idTo)) == null) {
			throw new RequiredFieldException("id");
		}
		return contributorService.mergeContributors(idFrom, idTo);
	}
}
