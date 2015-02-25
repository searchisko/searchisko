/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.filter;

import java.io.IOException;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

/**
 * CDI Producer of HTTP request. When in place we can inject HTTP request via CDI. Note: direct injecting of HTTP
 * request should work from CDI 1.1 so once we upgrade we can consider getting rid of this class.
 * 
 * @author Lukas Vlcek
 */
@WebFilter(
        urlPatterns = "/*",
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD}
)
public class CDIServletRequestProducingListener implements Filter {

	private static ThreadLocal<HttpServletRequest> SERVLET_REQUESTS = new ThreadLocal<>();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing needed
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		if (servletRequest instanceof HttpServletRequest) {
			SERVLET_REQUESTS.set((HttpServletRequest) servletRequest);
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {
		SERVLET_REQUESTS.remove();
	}

	@Produces
	@RequestScoped
	private HttpServletRequest produce() {
		return SERVLET_REQUESTS.get();
	}
}
