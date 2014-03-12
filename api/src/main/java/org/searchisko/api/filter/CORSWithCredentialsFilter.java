package org.searchisko.api.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.searchisko.api.util.SearchUtils;

/**
 * This is a simple filter that handles <a href="http://www.w3.org/TR/cors/">Cross-Origin Resource Sharing (CORS)</a>,
 * so Searchisko REST API can be used from javascript client app hosted on another domain.
 * 
 * @author Lukas Vlcek (lvlcek@redhat.com)
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class CORSWithCredentialsFilter implements Filter {

	/**
	 * Basic set of HTTP Request Methods.
	 * 
	 * @link http://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods
	 */
	public static final String OPTIONS = "OPTIONS";
	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final String PUT = "PUT";
	public static final String DELETE = "DELETE";

	/**
	 * CORS headers
	 */
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {

		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final HttpServletResponse httpResponse = (HttpServletResponse) response;

		String origin = SearchUtils.trimToNull(httpRequest.getHeader("Origin"));
		if (origin != null) {
			// it is CORS request only in case Origin request header is present
			httpResponse.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
			httpResponse.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

			// CORS Preflight Request handling
			if (OPTIONS.equals(httpRequest.getMethod())) {
				// allow to cache pre-flight response for 24 hours
				httpResponse.setHeader(ACCESS_CONTROL_MAX_AGE, "86400");
				httpResponse.setHeader(ACCESS_CONTROL_ALLOW_HEADERS,
						"X-Requested-With, Content-Type, Content-Length, Origin, Accept");

				httpResponse.addHeader(ACCESS_CONTROL_ALLOW_METHODS, GET);
				httpResponse.addHeader(ACCESS_CONTROL_ALLOW_METHODS, POST);
				httpResponse.addHeader(ACCESS_CONTROL_ALLOW_METHODS, PUT);
				httpResponse.addHeader(ACCESS_CONTROL_ALLOW_METHODS, DELETE);

				// OPTION request handling is done, do not call chain
				httpResponse.setStatus(HttpServletResponse.SC_OK);
				return;
			}
		}

		chain.doFilter(request, response);

	}

	@Override
	public void destroy() {
	}
}
