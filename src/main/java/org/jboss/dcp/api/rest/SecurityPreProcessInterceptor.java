/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.annotations.security.ProviderAllowed;
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

/**
 * Security REST pre processor to handle security annotations used for authorization. This preprocessor needs to be
 * placed after {@link AuthenticationInterceptor} which is done thanks to {@link HeaderDecoratorPrecedence}.
 * <p>
 * This interceptor uses {@link GuestAllowed} and {@link ProviderAllowed} annotations placed on REST API implementing
 * classes and methods.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * @see SecurityInterceptor
 */
@Provider
@ServerInterceptor
@HeaderDecoratorPrecedence
public class SecurityPreProcessInterceptor implements PreProcessInterceptor, AcceptedByMethod {

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

		ProviderAllowed providerAllowedAnnotation = getProviderAlowedAnnotation(declaring, method);

		if (providerAllowedAnnotation != null) {
			log.fine("REST Security, method allowed only for "
					+ (providerAllowedAnnotation.superProviderOnly() ? "SUPER " : "") + "PROVIDER: "
					+ declaring.getCanonicalName() + "." + method.getName());
			return true;
		} else {
			log.fine("REST Security, method allowed to guest: " + declaring.getCanonicalName() + "." + method.getName());
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
	 * Get {@link ProviderAllowed} annotation from method or method's class or declaring class.<br/>
	 * Precedence is:<br/>
	 * 1. Method<br/>
	 * 2. Method's class (declaring method can be some generic super class)<br/>
	 * 3. Declaring class
	 * 
	 * @param declaring
	 * @param method
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ProviderAllowed getProviderAlowedAnnotation(Class declaring, Method method) {
		if (method.isAnnotationPresent(ProviderAllowed.class)) {
			return method.getAnnotation(ProviderAllowed.class);
		} else {
			if (declaring.isAnnotationPresent(ProviderAllowed.class)) {
				return (ProviderAllowed) declaring.getAnnotation(ProviderAllowed.class);
			} else {
				if (method.getDeclaringClass().isAnnotationPresent(ProviderAllowed.class)) {
					return (ProviderAllowed) method.getDeclaringClass().getAnnotation(ProviderAllowed.class);
				}
			}
		}
		return null;
	}

	/**
	 * Is called for methods which require restricted access, necessary authorization check is done here. Authenticated
	 * Principal is prepared by {@link AuthenticationInterceptor} called before this and stored in {@link SecurityContext}
	 * . .
	 * 
	 */
	@Override
	public ServerResponse preProcess(HttpRequest request, ResourceMethod method) throws Failure, WebApplicationException {

		Principal principal = securityContext.getUserPrincipal();

		if (principal == null) {
			ServerResponse response = new ServerResponse();
			response.setStatus(HttpResponseCodes.SC_UNAUTHORIZED);
			response.getMetadata().add("WWW-Authenticate", "Basic realm=\"Insert Provider's username and password\"");
			return response;
		}

		// Now we have only ProviderAllowed annotation and this preprocessor is processed only for this annotation.
		// Because of that providerService.authenticate also check that it's provider who is trying to do operation

		// Check if provider must be super provider
		ProviderAllowed providerAllowed = getProviderAlowedAnnotation(method.getResourceClass(), method.getMethod());

		// Check roles
		if (providerAllowed.superProviderOnly() && !securityContext.isUserInRole(CustomSecurityContext.SUPER_ADMIN_ROLE)) {
			ServerResponse response = new ServerResponse();
			response.setStatus(HttpResponseCodes.SC_FORBIDDEN);
			return response;
		}

		return null;
	}

}
