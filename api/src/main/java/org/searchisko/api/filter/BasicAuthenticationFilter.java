package org.searchisko.api.filter;

import org.apache.commons.codec.binary.Base64;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filter consuming HTTP Basic Authentication and if present then requires login.
 * HTTP Basic Challenge is handled on REST layer in ProviderHttpBasicAuthInterceptor
 *
 * @author Libor Krzyzanek
 * @see javax.servlet.http.HttpServletRequest#login(String, String)
 * @see org.searchisko.api.rest.security.HttpBasicChallengeInterceptor
 */
public class BasicAuthenticationFilter implements Filter {

	@Inject
	protected Logger log;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.log(Level.INFO, "Initializing HTTP Basic Authentication Filter");
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		// Check http basic authentication first
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Request headers: {0}", Collections.list(request.getHeaderNames()));
			log.log(Level.FINEST, "Basic Authentication, authentications: {0}", Collections.list(request.getHeaders("Authorization")));
		}

		Enumeration<String> authentication = request.getHeaders("Authorization");
		while (authentication.hasMoreElements()) {
			String auth = authentication.nextElement();
			log.log(Level.FINEST, "Basic Authentication, examining: {0}", auth);
			if (!auth.startsWith("Basic")) {
				continue;
			}
			String hash = auth.substring(6);

			// Alternatively use org.jboss.resteasy.util.Base64
			byte[] decoded = Base64.decodeBase64(hash);
			String usernamePassword = new String(decoded);

			int colon = usernamePassword.indexOf(':');
			if (colon > 0) {
				String username = usernamePassword.substring(0, colon);
				String password = usernamePassword.substring(colon + 1, usernamePassword.length());

				log.log(Level.FINE, "Requiring Basic Authentication for username: {0}", username);
				try {
					request.login(username, password);
					log.log(Level.FINE, "Authenticated request: {0}", request.getUserPrincipal());
				} catch (final ServletException e) {
					log.log(Level.FINE, "Custom authentication failed.", e);
					response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
				}
			}
		}
		chain.doFilter(req, resp);
	}

	@Override
	public void destroy() {

	}
}
