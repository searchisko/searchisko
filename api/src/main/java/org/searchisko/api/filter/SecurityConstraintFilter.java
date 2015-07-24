/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.searchisko.api.rest.security.HttpBasicChallengeInterceptor;

/**
 * Common filter to access control based on roles
 *
 * @author Libor Krzyzanek
 */
public class SecurityConstraintFilter implements Filter {

	protected static final String CFG_ALLOWED_ROLES = "allowedRoles";

	@Inject
	protected Logger log;

	protected List<String> allowedRoles;


	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		String allowedRolesStr = filterConfig.getInitParameter(CFG_ALLOWED_ROLES);
		if (StringUtils.isNotBlank(allowedRolesStr)) {
			allowedRoles = Arrays.asList(allowedRolesStr.split(","));
		} else {
			throw new ServletException("Bad servlet configuration. Parameter " + CFG_ALLOWED_ROLES + " cannot be empty");
		}
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {

		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final HttpServletResponse httpResponse = (HttpServletResponse) response;

		boolean userInRole = false;
		for (String role : allowedRoles) {
			if (httpRequest.isUserInRole(role)) {
				log.log(Level.FINE, "User is in allowed role: " + role);
				userInRole = true;
				break;
			}
		}

		if (userInRole) {
			chain.doFilter(request, response);
			return;
		} else {
			if (httpRequest.getUserPrincipal() == null) {
				log.log(Level.FINE, "User is not authenticated");
				httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"" + HttpBasicChallengeInterceptor.CHALLENGE_TEXT
						+ "\"");
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			} else {
				log.log(Level.FINE, "User has no permission");
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

		}
	}

	@Override
	public void destroy() {
	}

}
