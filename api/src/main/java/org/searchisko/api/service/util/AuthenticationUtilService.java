/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.SecurityContext;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.searchisko.api.service.ContributorProfileService;

/**
 * Authentication utility service.
 * <p>
 * TODO _RATING refactoring necessary - remove this service and implement {@link PreProcessInterceptor} and
 * {@link SecurityContext} and other common authentication/authorization stuff instead.
 * 
 * @author Lukas Vlcek
 * @author Libor Krzyzanek
 */
@RequestScoped
public class AuthenticationUtilService {

	@Inject
	protected Logger log;

	// see CDIServletRequestProducingListener
	// injecting http servlet request should work from CDI 1.1
	@Inject
	protected HttpServletRequest request;

	@Inject
	protected ContributorProfileService contributorProfileService;

	/**
	 * Returns name of authenticated user
	 * 
	 * @return name of authenticated user or null is user is not authenticated or if HTTP session does not exist.
	 */
	public String getAuthenticatedUserName() {
		if (request != null) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Assertion assertion = (Assertion) session.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);
				if (assertion != null) {
					AttributePrincipal principal = assertion.getPrincipal();
					return principal == null ? null : principal.getName();
				} else {
					log.log(Level.FINEST, "No Assertion found in HTTP session");
				}
			} else {
				log.log(Level.FINEST, "Can not verify user authentication, no HTTP session found");
			}
		} else {
			log.log(Level.FINEST, "Can not verify user authentication, no HTTP request found");
		}
		return null;
	}

	/**
	 * Get Contributor Id for currently authenticated user
	 * 
	 * @return Contributor Id
	 */
	public String getAuthenticatedContributorId() {
		String currentUsername = getAuthenticatedUserName();
		if (currentUsername == null) {
			return null;
		}

		return contributorProfileService.getContributorId(currentUsername);
	}
}
