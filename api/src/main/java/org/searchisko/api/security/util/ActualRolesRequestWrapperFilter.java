/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */

package org.searchisko.api.security.util;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.searchisko.api.security.jaas.PrincipalWithRoles;

/**
 * Request wrapper which improves {@link javax.servlet.http.HttpServletRequest#isUserInRole(String)} method by providing
 * match against actual roles stored in cache or session by {@link org.searchisko.api.security.util.ActualRolesService}.
 *
 * @author Libor Krzyzanek
 * @see org.searchisko.api.security.util.ActualRolesService#getActualRolesRemoveFromCache(java.security.Principal)
 */
public class ActualRolesRequestWrapperFilter implements Filter {

	@Inject
	protected ActualRolesService actualRolesService;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		RolesInSessionHttpServletRequestWrapper wrapper = new RolesInSessionHttpServletRequestWrapper((HttpServletRequest) request, actualRolesService);
		chain.doFilter(wrapper, response);
	}

	@Override
	public void destroy() {
	}

	/**
	 * Wrapper improves #getUserPrincipal by updating roles in PrincipalWithRoles if any found in
	 * {@link org.searchisko.api.security.util.ActualRolesService}
	 * Improved #isUserInRole to check actual roles from PrincipalWithRoles
	 *
	 * @author Libor Krzyzanek
	 */
	public class RolesInSessionHttpServletRequestWrapper extends HttpServletRequestWrapper {

		public static final String SESSION_ACTUAL_ROLES_KEY = "actual_roles_from_update";

		private ActualRolesService actualRolesService;

		public RolesInSessionHttpServletRequestWrapper(HttpServletRequest request, ActualRolesService actualRolesService) {
			super(request);
			this.actualRolesService = actualRolesService;
		}

		/**
		 * Returns {@link org.searchisko.api.security.jaas.PrincipalWithRoles} with updated roles
		 * if any found in cache/session
		 *
		 * @return
		 */
		@Override
		public Principal getUserPrincipal() {
			Principal principal = super.getUserPrincipal();
			if (principal == null) {
				return null;
			}
			HttpServletRequest req = (HttpServletRequest) getRequest();

			Set<String> roles = getRolesFromCacheOrSession(req, principal);
			if (roles != null) {
				// update roles in principal
				if (principal instanceof PrincipalWithRoles) {
					((PrincipalWithRoles) principal).setRoles(roles);
				}
			}

			return principal;
		}

		/**
		 * Overrides default implementation by checking if role is PrincipalWithRoles's roles
		 *
		 * @param role
		 * @return
		 */
		@Override
		public boolean isUserInRole(String role) {
			Principal principal = getUserPrincipal();
			if (principal instanceof PrincipalWithRoles) {
				return ((PrincipalWithRoles) principal).getRoles().contains(role);
			}

			return super.isUserInRole(role);
		}

		@SuppressWarnings("unchecked")
		protected Set<String> getRolesFromCacheOrSession(HttpServletRequest req, Principal principal) {
			Set<String> rolesFromCache = actualRolesService.getActualRolesRemoveFromCache(principal);
			if (rolesFromCache != null) {
				// keeps copy of roles in session for next requests. It's assumed that load balancer has configured sticky sessions
				// so we're sure that next call would go to same session
				req.getSession().setAttribute(SESSION_ACTUAL_ROLES_KEY, rolesFromCache);
				return rolesFromCache;
			}

            HttpSession session = req.getSession(false);
			if (session != null) {
                return (Set<String>) session.getAttribute(SESSION_ACTUAL_ROLES_KEY);
            }

			return null;
		}
	}
}
