/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.SecurityPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.server.embedded.SimplePrincipal;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.jboss.resteasy.util.Base64;
import org.searchisko.api.service.ProviderService;

/**
 * Interceptor handle authentication via standard HTTP basic authentication header or via url parameters called
 * <code>provider</code> and <code>pwd</code> and store authenticated {@link Principal} into {@link SecurityContext} to
 * be used later by authorization interceptor {@link SecurityPreProcessInterceptor} if necessary. Authentication check
 * is done using {@link ProviderService#authenticate(String, String)}.
 *
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
@Provider
@ServerInterceptor
@SecurityPrecedence
public class AuthenticationInterceptor implements PreProcessInterceptor {

	@Inject
	protected Logger log;

	@Inject
	protected ProviderService providerService;

	@Override
	public ServerResponse preProcess(HttpRequest request, ResourceMethod method) throws Failure, WebApplicationException {

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
					String usernamePassword = new String(decoded);

					int colon = usernamePassword.indexOf(':');
					if (colon > 0) {
						username = usernamePassword.substring(0, colon);
						password = usernamePassword.substring(colon + 1, usernamePassword.length());

						authenticated = providerService.authenticate(username, password);
						authenticationScheme = SecurityContext.BASIC_AUTH;
						break;
					}
				} catch (IOException e) {
					log.log(Level.SEVERE, "Cannot encode authentication realm", e);
				}

			}
		}

		if (authenticated) {
			Principal principal = new SimplePrincipal(username);
			ResteasyProviderFactory.pushContext(SecurityContext.class,
					new CustomSecurityContext(principal, providerService.isSuperProvider(username), true, authenticationScheme));
			log.log(Level.FINE, "Request authenticated. Username: {0}", username);
		}

		return null;

	}

}
