/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.util.AbstractCasFilter;
import org.jasig.cas.client.validation.Assertion;
import org.jboss.resteasy.annotations.interception.SecurityPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.searchisko.api.annotations.security.ContributorAllowed;
import org.searchisko.api.filter.CDIServletRequestProducingListener;
import org.searchisko.api.util.SearchUtils;

/**
 * This interceptor handle Contributor ( {@link AuthenticatedUserType#CONTRIBUTOR}) authentication. for now via CAS web
 * SSO login. It stores authenticated {@link Principal} with CAS usernamename in
 * {@link ContributorCustomSecurityContext} instance into {@link SecurityContext} to be used later by authorization
 * interceptor {@link ContributorSecurityPreProcessInterceptor} if necessary.
 *
 * @author Vlastimil Elias (velias at redhat dot com)
 */
@Provider
@ServerInterceptor
@SecurityPrecedence
public class ContributorAuthenticationInterceptor implements PreProcessInterceptor, AcceptedByMethod {

	public static String AUTH_METHOD_CAS = "CAS_SSO";

	@Inject
	protected Logger log;

	/**
	 * @see CDIServletRequestProducingListener
	 */
	@Inject
	protected HttpServletRequest servletRequest;

	/**
	 * Returns false for methods which are not restricted to Contributors only, so authentication check in
	 * {@link #preProcess(HttpRequest, ResourceMethod)} is not called for them - it is called only if Contributor
	 * authentication is necessary!
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean accept(Class declaring, Method method) {
		if (!ProviderSecurityPreProcessInterceptor.isGuestAllowed(method)) {

			ContributorAllowed allowedAnnotation = ContributorSecurityPreProcessInterceptor.getContributorAllowedAnnotation(
					declaring, method);

			if (allowedAnnotation != null) {
				log.fine("REST Security, method restricted to Contributor only, go to perform authentication check: "
						+ declaring.getCanonicalName() + "." + method.getName());
				return true;
			}
		}

		log.fine("REST Security, method not restricted to Contributors only, skip authentication check: "
				+ declaring.getCanonicalName() + "." + method.getName());
		return false;

	}

	@Override
	public ServerResponse preProcess(HttpRequest request, ResourceMethod method) throws Failure, WebApplicationException {

		String username = null;
		String authenticationScheme = null;
		if (servletRequest != null) {
			HttpSession session = servletRequest.getSession(false);
			if (session != null) {
				Assertion assertion = (Assertion) session.getAttribute(AbstractCasFilter.CONST_CAS_ASSERTION);
				if (assertion != null) {
					AttributePrincipal principal = assertion.getPrincipal();
					if (principal != null) {
						username = SearchUtils.trimToNull(principal.getName());
						if (username != null)
							authenticationScheme = AUTH_METHOD_CAS;
					}
				} else {
					log.log(Level.FINEST, "No CAS Assertion found in HTTP session");
				}
			} else {
				log.log(Level.FINEST, "Can not verify CAS user authentication, no HTTP session found");
			}
		} else {
			log.log(Level.SEVERE, "Can not verify CAS user authentication, no HTTP request found");
			throw new RuntimeException("HttpServletRequest not injected");
		}
		if (authenticationScheme != null) {
			Principal principal = new SimplePrincipal(username);
			ResteasyProviderFactory.pushContext(SecurityContext.class, new ContributorCustomSecurityContext(principal, true,
					authenticationScheme));
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Request authenticated for Contributor with username {0} using method {1}", new Object[]{
						username, authenticationScheme});
			}
		}

		return null;

	}
}
