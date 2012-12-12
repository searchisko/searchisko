/**
 * 
 */
package org.jboss.dcp.api.rest;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.jboss.dcp.api.service.ProviderService;
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

/**
 * Interceptor handle authentication via standard HTTP basic authentication or via url parameters
 * 
 * @author Libor Krzyzanek
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
			if (queryParams != null) {
				username = queryParams.getFirst("provider");
				password = queryParams.getFirst("pwd");

				authenticated = providerService.authenticate(username, password);
				authenticationScheme = "CUSTOM";
			}
		}

		if (authenticated) {
			Principal principal = new SimplePrincipal(username);
			ResteasyProviderFactory.pushContext(SecurityContext.class, new CustomSecurityContext(principal,
					providerService.isSuperProvider(username), true, authenticationScheme));
			log.log(Level.FINE, "Request authenticated. Username: {0}", username);
		}

		return null;

	}

}
