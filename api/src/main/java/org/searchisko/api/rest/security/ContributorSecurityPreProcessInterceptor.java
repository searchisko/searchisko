/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.security;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.HeaderDecoratorPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.interceptors.SecurityInterceptor;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.searchisko.api.annotations.security.ContributorAllowed;
import org.searchisko.api.annotations.security.GuestAllowed;

/**
 * Security REST pre processor to handle security annotations used for Contributor authorization. This preprocessor
 * needs to be placed after {@link ContributorAuthenticationInterceptor} which is done thanks to
 * {@link HeaderDecoratorPrecedence}.
 * <p>
 * This interceptor uses {@link GuestAllowed} and {@link ContributorAllowed} annotations placed on REST API implementing
 * classes and methods.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see SecurityInterceptor
 */
@Provider
@ServerInterceptor
@HeaderDecoratorPrecedence
public class ContributorSecurityPreProcessInterceptor implements PreProcessInterceptor, AcceptedByMethod {

	@Inject
	protected Logger log;

	@Context
	protected SecurityContext securityContext;

	/**
	 * Returns false for methods which are allowed for all users (Guests) so
	 * {@link #preProcess(HttpRequest, ResourceMethod)} is not called for them.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean accept(Class declaring, Method method) {
		if (isGuestAllowed(method)) {
			log.fine("REST Security, method allowed to guest: " + declaring.getCanonicalName() + "." + method.getName());
			return false;
		}

		ContributorAllowed providerAllowedAnnotation = getContributorAllowedAnnotation(declaring, method);

		if (providerAllowedAnnotation != null && !providerAllowedAnnotation.optional()) {
			log.fine("REST Security, method allowed for Contributors only: " + declaring.getCanonicalName() + "."
					+ method.getName());
			return true;
		} else {
			log.fine("REST Security, method not restricted for contributors: " + declaring.getCanonicalName() + "."
					+ method.getName());
			return false;
		}
	}

	/**
	 * Check if Guest (unauthenticated) access is explicitly allowed/enforced for given method.
	 * 
	 * @param method to check
	 * @return true if guest access is allowed
	 */
	public static boolean isGuestAllowed(Method method) {
		return method.isAnnotationPresent(GuestAllowed.class);
	}

	/**
	 * Get {@link ContributorAllowed} annotation from method or method's class or declaring class.<br/>
	 * Precedence is:<br/>
	 * 1. Method<br/>
	 * 2. Method's class (declaring method can be some generic super class)<br/>
	 * 3. Declaring class
	 * 
	 * @param declaring
	 * @param method
	 * @return annotation instance or null if not found
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ContributorAllowed getContributorAllowedAnnotation(Class declaring, Method method) {
		if (method.isAnnotationPresent(ContributorAllowed.class)) {
			return method.getAnnotation(ContributorAllowed.class);
		} else {
			if (declaring.isAnnotationPresent(ContributorAllowed.class)) {
				return (ContributorAllowed) declaring.getAnnotation(ContributorAllowed.class);
			} else {
				if (method.getDeclaringClass().isAnnotationPresent(ContributorAllowed.class)) {
					return method.getDeclaringClass().getAnnotation(ContributorAllowed.class);
				}
			}
		}
		return null;
	}

	/**
	 * Is called for methods which require restricted access, necessary authorization check is done here. Authenticated
	 * Principal is prepared by {@link ProviderAuthenticationInterceptor} called before this and stored in
	 * {@link SecurityContext} . .
	 * 
	 */
	@Override
	public ServerResponse preProcess(HttpRequest request, ResourceMethod method) throws Failure, WebApplicationException {

		log.fine("REST Security, Contributor security check for security context: " + securityContext);

		if (securityContext == null || securityContext.getUserPrincipal() == null
				|| !securityContext.isUserInRole(AuthenticatedUserType.CONTRIBUTOR.roleName())) {
			ServerResponse response = new ServerResponse();
			response.setStatus(HttpResponseCodes.SC_FORBIDDEN);
			response.setEntity("Contributor authentication required");
			return response;
		}

		return null;
	}

}
