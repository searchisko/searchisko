package org.searchisko.api.filter;

import java.io.IOException;
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

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.plugins.providers.jackson.JacksonJsonpInterceptor;

/**
 * Filter restricting use of JSONP (provided by {@link JacksonJsonpInterceptor} ) for nonauthenticated users only and
 * not for <code>/auth/status</code> due security reasons - see <a
 * href="https://github.com/searchisko/searchisko/issues/159">#159</a>
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class JSONPRestrictionFilter implements Filter {

	public static final String PARAM_CALLBACK = JacksonJsonpInterceptor.DEFAULT_CALLBACK_QUERY_PARAMETER;

	@Inject
	protected Logger log;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.log(Level.INFO, "Initializing JSONP restriction Filter");
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
			ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		// JSONP is only on GET
		if (CORSWithCredentialsFilter.GET.equals(request.getMethod())
				&& StringUtils.isNotBlank(request.getParameter(PARAM_CALLBACK))) {
			if (request.getRequestURI().endsWith("/auth/status") || request.getUserPrincipal() != null) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN,
						"JSONP is not allowed for authenticated user and for /auth/status due security reasons");
				return;
			}
		}
		chain.doFilter(req, resp);
	}

	@Override
	public void destroy() {

	}
}
