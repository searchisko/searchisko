/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.searchisko.api.audit.annotation.Audit;
import org.searchisko.api.audit.annotation.AuditIgnore;
import org.searchisko.api.security.Role;
import org.searchisko.api.service.SearchIndexMissingException;
import org.searchisko.api.service.StatsClientService;
import org.searchisko.api.service.StatsRecordType;
import org.searchisko.api.service.SystemInfoService;

/**
 * System related REST service
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Path("/sys")
@RequestScoped
@RolesAllowed(Role.PROVIDER)
@Audit
public class SystemRestService {

	@Context
	protected SecurityContext securityContext;

	@Inject
	private SystemInfoService systemInfoService;

	@Inject
	private StatsClientService statsClientService;

	@GET
	@Path("/info")
	@Produces(MediaType.APPLICATION_JSON)
	@PermitAll
	public Map<Object, Object> info() throws IOException {
		return systemInfoService.getSystemInfo(securityContext.isUserInRole(Role.ADMIN));
	}

	@GET
	@Path("/auditlog")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed(Role.ADMIN)
	@AuditIgnore
	public StreamingOutput getAuditLog(
			@QueryParam("operation") String operation,
			@QueryParam("path") String path,
			@QueryParam("username") String username,
			@QueryParam("usertype") String usertype,
			@QueryParam("id") String id,
			@QueryParam("from") Integer from,
			@QueryParam("size") Integer size,
			@QueryParam("sort") String sort) throws IOException {


		BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter();
		boolean filterAdded = false;
		if (StringUtils.isNotBlank(path)) {
			filterAdded = true;
			filterBuilder.must(FilterBuilders.prefixFilter("path", path));
		}


		if (StringUtils.isNotBlank(operation)) {
			filterAdded = true;
			filterBuilder.must(FilterBuilders.termFilter("operation", operation));
		}
		if (StringUtils.isNotBlank(username)) {
			filterAdded = true;
			filterBuilder.must(FilterBuilders.termFilter("username", username));
		}
		if (StringUtils.isNotBlank(usertype)) {
			filterAdded = true;
			filterBuilder.must(FilterBuilders.termFilter("usertype", usertype));
		}
		if (StringUtils.isNotBlank(id)) {
			filterAdded = true;
			filterBuilder.must(FilterBuilders.termFilter("id", id));
		}
		if (!filterAdded) {
			filterBuilder = null;
		}

		try {
			SearchResponse response = statsClientService.performSearch(StatsRecordType.AUDIT, filterBuilder, from, size, sort);
			return new ESDataOnlyResponse(response);
		} catch (SearchIndexMissingException e) {
			return null;
		}
	}

}
