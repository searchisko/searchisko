package org.searchisko.api.filter;

import org.searchisko.api.rest.CORSSupportInterceptor;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This is a simple filter that is needed to allow CORS with credentials.
 * <p/>
 * According to CORS specification if response contains 'Access-Control-Allow-Credentials' set to 'true' then
 * response header 'Access-Control-Allow-Origin' must match 'Origin' value found in request.
 * <p/>
 * CORS related headers are added by {@link org.searchisko.api.rest.CORSSupportInterceptor} which is executed only once
 * during deployment time and it sets value of 'Access-Control-Allow-Origin' header to '*' if method was annotated with
 * {@link org.searchisko.api.annotations.header.CORSSupport} annotation. Thus the value can not be changed dynamically
 * so we do it in this filter.
 *
 * @author Lukas Vlcek (lvlcek@redhat.com)
 */
@WebFilter("/*")
public class CORSWithCredentialsFilter implements Filter {
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing needed
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		// Process the filter chain first
		chain.doFilter(request, response);

		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final HttpServletResponse httpResponse = (HttpServletResponse) response;

		String withCredentials = httpResponse.getHeader(CORSSupportInterceptor.ACCESS_CONTROL_ALLOW_CREDENTIALS);
		if (withCredentials != null && withCredentials.trim().equals("true")) {
			String origin = httpRequest.getHeader("Origin");
			if (origin != null) {
				httpResponse.setHeader(CORSSupportInterceptor.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
			}
		}
	}

	@Override
	public void destroy() {
		// Nothing needed
	}
}
