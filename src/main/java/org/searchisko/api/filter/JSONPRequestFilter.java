/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * <p>
 * This filter wraps qualified requests for JSON content with that standard JSONP padding. This makes these calls
 * accessible cross-domain using standard JSONP approaches.
 * </p>
 * <p>
 * To qualify for wrapping the request must be made to the <i>/rest/*</i> path, and contain a query parameter call
 * <i>jsoncallback</> that defines the JSONP callback method to use with the response.
 * </p>
 * <p>
 * It's taken from Aerogear project because RestEasy doesn't provide JSONP
 * (https://issues.jboss.org/browse/RESTEASY-342)
 * https://raw.github.com/aerogear/as-quickstarts/master/kitchensink-html5-mobile/src/main
 * /java/org/jboss/as/quickstarts/html5_mobile/util/JSONPRequestFilter.java
 * </p>
 * 
 * 
 * @author balunasj
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
@WebFilter("/*")
public class JSONPRequestFilter implements Filter {
	// The callback method to use
	private static final String CALLBACK_METHOD = "callback";

	// This is a simple safe pattern check for the callback method
	public static final Pattern SAFE_PRN = Pattern.compile("[a-zA-Z0-9_\\.]+");

	public static final String CONTENT_TYPE = "application/javascript";

	@Override
	public void init(FilterConfig config) throws ServletException {
		// Nothing needed
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {

		if (!(request instanceof HttpServletRequest)) {
			throw new ServletException("Only HttpServletRequest requests are supported");
		}

		final HttpServletRequest httpRequest = (HttpServletRequest) request;
		final HttpServletResponse httpResponse = (HttpServletResponse) response;

		// extract the callback method from the request query parameters
		String callback = getCallbackMethod(httpRequest);

		if (!isJSONPRequest(callback)) {
			// Request is not a JSONP request move on
			chain.doFilter(request, response);
		} else {
			// Need to check if the callback method is safe
			if (!SAFE_PRN.matcher(callback).matches()) {
				throw new ServletException("JSONP Callback method '" + CALLBACK_METHOD + "' parameter not valid function");
			}

			// Will stream updated response
			final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

			// Create a custom response wrapper to adding in the padding
			HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(httpResponse) {
				PrintWriter pw = null;

				@Override
				public ServletOutputStream getOutputStream() throws IOException {
					return new ServletOutputStream() {
						@Override
						public void write(int b) throws IOException {
							byteStream.write(b);
						}
					};
				}

				@Override
				public PrintWriter getWriter() throws IOException {
					if (pw == null)
						pw = new PrintWriter(byteStream);
					return pw;
				}

				@Override
				public void flushBuffer() throws IOException {
					if (pw != null)
						pw.flush();
				}
			};

			// Process the rest of the filter chain, including the JAX-RS request
			chain.doFilter(request, responseWrapper);

			// Handle flush of PrintWriter buffer used in wrapper
			responseWrapper.flushBuffer();

			// Override response content and encoding
			response.setContentType(CONTENT_TYPE);
			response.setCharacterEncoding("UTF-8");

			// Write the padded updates to the output stream.
			response.getOutputStream().write((callback + "(").getBytes());
			response.getOutputStream().write(byteStream.toByteArray());
			response.getOutputStream().write(");".getBytes());
		}
	}

	protected String getCallbackMethod(HttpServletRequest httpRequest) {
		return httpRequest.getParameter(CALLBACK_METHOD);
	}

	protected boolean isJSONPRequest(String callbackMethod) {
		// A simple check to see if the query parameter has been set.
		return (callbackMethod != null && callbackMethod.length() > 0);
	}

	@Override
	public void destroy() {
		// Nothing to do
	}
}
