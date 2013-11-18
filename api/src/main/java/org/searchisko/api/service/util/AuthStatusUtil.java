/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.service.util;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Lukas Vlcek
 */
@ApplicationScoped
public class AuthStatusUtil {

	@Inject
	protected Logger log;

	// see CDIServletRequestProducingListener
	// injecting http servlet request should work from CDI 1.1
	@Inject
	protected HttpServletRequest request;

	/**
	 * Returns name of authenticated user. Returns null is user is not authenticated or if HTTP session does not exist.
	 * @return name of authenticated user
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
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "No Assertion found in HTTP session");
					}
				}
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Can not verify user authentication, no HTTP session found");
				}
			}
		} else {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Can not verify user authentication, no HTTP request found");
			}
		}
		return null;
	}
}
