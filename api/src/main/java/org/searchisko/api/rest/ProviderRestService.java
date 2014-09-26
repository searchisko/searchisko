/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.ejb.ObjectNotFoundException;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.searchisko.api.audit.annotation.Audit;
import org.searchisko.api.audit.annotation.AuditContent;
import org.searchisko.api.audit.annotation.AuditId;
import org.searchisko.api.audit.annotation.AuditIgnore;
import org.searchisko.api.rest.exception.NotAuthorizedException;
import org.searchisko.api.rest.exception.RequiredFieldException;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.AuthenticationUtilService;
import org.searchisko.api.service.ContentManipulationLockService;
import org.searchisko.api.service.ProviderService;
import org.searchisko.api.service.SecurityService;

/**
 * Provider REST API
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@RequestScoped
@Path("/provider")
@RolesAllowed(Role.ADMIN)
@Audit
public class ProviderRestService extends RestEntityServiceBase {

	protected static final String[] FIELDS_TO_REMOVE = new String[] { ProviderService.PASSWORD_HASH };

	@Inject
	protected ProviderService providerService;

	@Inject
	protected SecurityService securityService;

	@Inject
	protected AuthenticationUtilService authenticationUtilService;

	@Inject
	protected ContentManipulationLockService contentManipulationLockService;

	@PostConstruct
	public void init() {
		setEntityService(providerService);
	}

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@AuditIgnore
	public Object getAll(@QueryParam("from") Integer from, @QueryParam("size") Integer size) {
		return entityService.getAll(from, size, FIELDS_TO_REMOVE);
	}

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ Role.ADMIN, Role.PROVIDER })
	@Override
	@AuditIgnore
	public Object get(@PathParam("id") String id) {

		if (StringUtils.isBlank(id)) {
			throw new RequiredFieldException("id");
		}

		Map<String, Object> entity = entityService.get(id);

		if (entity == null)
			return Response.status(Status.NOT_FOUND).build();

		String usernameOfProviderWeChange = entity.get(ProviderService.NAME).toString();

		authenticationUtilService.checkProviderManagementPermission(usernameOfProviderWeChange);

		return ESDataOnlyResponse.removeFields(entity, FIELDS_TO_REMOVE);
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(@AuditContent Map<String, Object> data) {
		String nameFromData = (String) data.get(ProviderService.NAME);
		if (StringUtils.isBlank(nameFromData))
			return Response.status(Status.BAD_REQUEST).entity("Required data field '" + ProviderService.NAME + "' not set")
					.build();
		return this.create(nameFromData, data);
	}

	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Object create(@PathParam("id") @AuditId String id, @AuditContent Map<String, Object> data) {

		if (StringUtils.isBlank(id)) {
			throw new RequiredFieldException("id");
		}

		String nameFromData = (String) data.get(ProviderService.NAME);
		if (StringUtils.isBlank(nameFromData))
			return Response.status(Status.BAD_REQUEST).entity("Required data field '" + ProviderService.NAME + "' not set")
					.build();

		if (!id.equals(nameFromData)) {
			return Response.status(Status.BAD_REQUEST)
					.entity("Name in URL must be same as '" + ProviderService.NAME + "' field in data.").build();
		}

		// do not update password hash if entity exists already!
		Map<String, Object> entity = providerService.get(id);
		if (entity != null) {
			Object pwdhash = entity.get(ProviderService.PASSWORD_HASH);
			if (pwdhash != null)
				data.put(ProviderService.PASSWORD_HASH, pwdhash);
		}
		providerService.create(id, data);
		return createResponseWithId(id);
	}

	@POST
	@Path("/{id}/password")
	@RolesAllowed({ Role.ADMIN, Role.PROVIDER })
	public Object changePassword(@PathParam("id") @AuditId String id, String pwd) {

		if (StringUtils.isBlank(id)) {
			throw new RequiredFieldException("id");
		}

		if (StringUtils.isBlank(pwd)) {
			throw new RequiredFieldException("pwd");
		}

		Map<String, Object> entity = entityService.get(id);

		if (entity == null)
			return Response.status(Status.NOT_FOUND).build();

		String usernameOfProviderWeChange = entity.get(ProviderService.NAME).toString();

		authenticationUtilService.checkProviderManagementPermission(usernameOfProviderWeChange);

		entity.put(ProviderService.PASSWORD_HASH, securityService.createPwdHash(usernameOfProviderWeChange, pwd.trim()));
		entityService.update(id, entity);

		return Response.ok().build();
	}

	@POST
	@Path("/{id}/content_manipulation_lock")
	@RolesAllowed({ Role.ADMIN, Role.PROVIDER })
	public Object contentManipulationLockCreate(@PathParam("id") @AuditId String id) throws ObjectNotFoundException {

		if (StringUtils.isBlank(id)) {
			throw new RequiredFieldException("id");
		}

		if (ContentManipulationLockService.API_ID_ALL.equals(id)) {
			if (!authenticationUtilService.isUserInRole(Role.ADMIN)) {
				throw new NotAuthorizedException("admin permission required");
			}
			contentManipulationLockService.createLockAll();

		} else {
			Map<String, Object> entity = entityService.get(id);

			if (entity == null)
				throw new ObjectNotFoundException();

			String usernameOfProviderWeChange = entity.get(ProviderService.NAME).toString();
			authenticationUtilService.checkProviderManagementPermission(usernameOfProviderWeChange);

			contentManipulationLockService.createLock(id);
		}
		return Response.ok().build();
	}

	@DELETE
	@Path("/{id}/content_manipulation_lock")
	@RolesAllowed({ Role.ADMIN, Role.PROVIDER })
	public Object contentManipulationLockDelete(@PathParam("id") @AuditId String id) throws ObjectNotFoundException {

		if (StringUtils.isBlank(id)) {
			throw new RequiredFieldException("id");
		}

		if (ContentManipulationLockService.API_ID_ALL.equals(id)) {
			if (!authenticationUtilService.isUserInRole(Role.ADMIN)) {
				throw new NotAuthorizedException("admin permission required");
			}
			contentManipulationLockService.removeLockAll();
		} else {
			Map<String, Object> entity = entityService.get(id);

			if (entity == null)
				throw new ObjectNotFoundException();

			String usernameOfProviderWeChange = entity.get(ProviderService.NAME).toString();
			authenticationUtilService.checkProviderManagementPermission(usernameOfProviderWeChange);

			if (!contentManipulationLockService.removeLock(id)) {
				throw new NotAuthorizedException("admin permission required");
			}
		}
		return Response.ok().build();
	}

	@GET
	@Path("/{id}/content_manipulation_lock")
	@RolesAllowed({ Role.ADMIN, Role.PROVIDER })
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> contentManipulationLockInfo(@PathParam("id") @AuditId String id)
			throws ObjectNotFoundException {

		if (StringUtils.isBlank(id)) {
			throw new RequiredFieldException("id");
		}

		List<String> ret = null;

		if (ContentManipulationLockService.API_ID_ALL.equals(id)) {
			if (!authenticationUtilService.isUserInRole(Role.ADMIN)) {
				throw new NotAuthorizedException("admin permission required");
			}
			ret = contentManipulationLockService.getLockInfo();
		} else {
			Map<String, Object> entity = entityService.get(id);

			if (entity == null)
				throw new ObjectNotFoundException();

			String usernameOfProviderWeChange = entity.get(ProviderService.NAME).toString();
			authenticationUtilService.checkProviderManagementPermission(usernameOfProviderWeChange);

			List<String> allRet = contentManipulationLockService.getLockInfo();
			if (allRet != null) {
				if (allRet.contains(id)) {
					ret = new ArrayList<>();
					ret.add(id);
				} else if (allRet.contains(ContentManipulationLockService.API_ID_ALL)) {
					ret = new ArrayList<>();
					ret.add(ContentManipulationLockService.API_ID_ALL);
				}
			}
		}
		Map<String, Object> retMap = new HashMap<>();
		if (ret != null && !ret.isEmpty())
			retMap.put("content_manipulation_lock", ret);
		return retMap;
	}

}
