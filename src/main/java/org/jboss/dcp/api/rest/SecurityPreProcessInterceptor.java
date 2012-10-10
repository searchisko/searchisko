/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.io.IOException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.jboss.dcp.api.annotations.security.GuestAllowed;
import org.jboss.dcp.api.annotations.security.ProviderAllowed;
import org.jboss.dcp.api.service.ProviderService;
import org.jboss.resteasy.annotations.interception.SecurityPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.interceptors.SecurityInterceptor;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.jboss.resteasy.util.Base64;
import org.jboss.resteasy.util.HttpResponseCodes;

/**
 * Security REST pre processor to handle security annotations
 * 
 * @author Libor Krzyzanek
 * @see SecurityInterceptor
 */
@Provider
@ServerInterceptor
@SecurityPrecedence
public class SecurityPreProcessInterceptor implements PreProcessInterceptor, AcceptedByMethod {

	@Inject
	private Logger log;

	@Inject
	private ProviderService providerService;

	@SuppressWarnings("rawtypes")
	@Override
	public boolean accept(Class declaring, Method method) {
		boolean guestAllowed = method.isAnnotationPresent(GuestAllowed.class);
		if (guestAllowed) {
			log.info("REST Security, method allowed to guest: " + declaring.getCanonicalName() + "." + method.getName());
			return false;
		}

		ProviderAllowed providerAllowedAnnotation = getProviderAlowedAnnotation(declaring, method);

		if (providerAllowedAnnotation != null) {
			log.info("REST Security, method allowed only for "
					+ (providerAllowedAnnotation.superProviderOnly() ? "SUPER " : "") + "PROVIDER: "
					+ declaring.getCanonicalName() + "." + method.getName());
			return true;
		} else {
			log.info("REST Security, method allowed to guest: " + declaring.getCanonicalName() + "." + method.getName());
			return false;
		}
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
	private ProviderAllowed getProviderAlowedAnnotation(Class declaring, Method method) {
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

	@Override
	public ServerResponse preProcess(HttpRequest request, ResourceMethod method) throws Failure,
			WebApplicationException {

		boolean authenticated = false;
		String authenticationScheme = null;
		String username = null;
		String password;
		// Check http basic authentication first
		List<String> authentication = request.getHttpHeaders().getRequestHeader("Authorization");
		if (authentication != null && authentication.size() > 0) {
			for (String auth : authentication) {
				if (!auth.startsWith("Basic")) {
					continue;
				}
				String hash = auth.substring(6);

				try {
					byte[] decoded = Base64.decode(hash);
					// TODO: CHarset ???
					String usernamePassword = new String(decoded);

					int colomn = usernamePassword.indexOf(':');
					if (colomn > 0) {
						username = usernamePassword.substring(0, colomn);
						password = usernamePassword.substring(colomn + 1, usernamePassword.length());

						authenticated = providerService.authenticate(username, password);
						authenticationScheme = SecurityContext.BASIC_AUTH;
						break;
					}
				} catch (IOException e) {
					log.log(Level.SEVERE, "Cannot encode authentication realm", e);
				}

			}
		}

		if (!authenticated) {
			// Check username and password as query parameters
			MultivaluedMap<String, String> queryParams = request.getUri().getQueryParameters();
			username = queryParams.getFirst("provider");
			password = queryParams.getFirst("pwd");

			authenticated = providerService.authenticate(username, password);
			authenticationScheme = "CUSTOM";
		}

		if (!authenticated) {
			ServerResponse response = new ServerResponse();
			response.setStatus(HttpResponseCodes.SC_UNAUTHORIZED);
			response.getMetadata().add("WWW-Authenticate", "Basic realm=\"Insert Provider's username and password\"");
			return response;
		}

		// Now we have only ProviderAllowed annotation and this preprocessor is processed only for this annotation.
		// Because of that providerService.authenticate also check that it's provider who is trying to do operation

		// Check if provider must be super provider
		ProviderAllowed providerAllowed = getProviderAlowedAnnotation(method.getResourceClass(), method.getMethod());

		Principal principal = new SimplePrincipal(username);
		ResteasyProviderFactory.pushContext(SecurityContext.class,
				new CustomSecurityContext(principal, providerAllowed.superProviderOnly(), true, authenticationScheme));

		// Check roles
		if (providerAllowed.superProviderOnly() && !providerService.isSuperProvider(username)) {
			ServerResponse response = new ServerResponse();
			response.setStatus(HttpResponseCodes.SC_FORBIDDEN);
			return response;
		}

		return null;
	}

}
