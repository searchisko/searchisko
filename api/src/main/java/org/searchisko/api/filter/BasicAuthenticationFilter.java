package org.searchisko.api.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;

/**
 * Filter consuming HTTP Basic Authentication and if present then requires login.
 * HTTP Basic Challenge is handled on REST layer in ProviderHttpBasicAuthInterceptor or on Filter/Servlet level.
 * <p></p>
 * Configuration parameters:<br/>
 * - excludedUrl - URL ending with this value will be excluded
 *
 * @author Libor Krzyzanek
 * @see javax.servlet.http.HttpServletRequest#login(String, String)
 * @see org.searchisko.api.rest.security.HttpBasicChallengeInterceptor
 */
public class BasicAuthenticationFilter implements Filter {

	@Inject
	protected Logger log;

	public static final String PARAM_EXCLUDED_URL = "excludedUrl";

	protected String excludedUrl;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.log(Level.INFO, "Initializing HTTP Basic Authentication Filter");
		setExcludedUrl(filterConfig.getInitParameter(PARAM_EXCLUDED_URL));
		log.log(Level.FINE, "Excluded URL: {0}", excludedUrl);
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		// Check http basic authentication first
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Request headers: {0}", Collections.list(request.getHeaderNames()));
			log.log(Level.FINEST, "Basic Authentication, authentications: {0}", Collections.list(request.getHeaders("Authorization")));
			log.log(Level.FINEST, "Current principal: {0}", request.getUserPrincipal());
		}

		if (excludedUrl != null && request.getRequestURI().endsWith(excludedUrl)) {
			log.log(Level.FINE, "URL ignored. URL: {0}", request.getRequestURI());
			chain.doFilter(req, resp);
			return;
		}

		if (request.getUserPrincipal() != null) {
			// #158 - Do not consume http basic authentication header when user is already authenticated
			log.log(Level.FINE, "Request already authenticated.");
			chain.doFilter(req, resp);
			return;
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
					return;
				}
			}
		}
		chain.doFilter(req, resp);
	}

	@Override
	public void destroy() {

	}

	public void setExcludedUrl(String excludedUrl) {
		this.excludedUrl = excludedUrl;
	}
}
