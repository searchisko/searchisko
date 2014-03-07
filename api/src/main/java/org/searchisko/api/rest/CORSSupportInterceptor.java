/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import org.searchisko.api.annotations.header.CORSSupport;
import org.jboss.resteasy.annotations.interception.HeaderDecoratorPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PostProcessInterceptor;

/**
 * RestEasy interceptor which adds '<code>Access-Control-Allow-Origin: *<code>' header to JAX-RS responses
 * for JAX-RS methods annotated by {@link org.searchisko.api.annotations.header.CORSSupport}.
 *
 * It can be used on any HTTP method (GET, PUT, POST, ... ) even on custom ones. If, however; used on OPTIONS
 * method then it adds also '<code>Access-Control-Allow-Methods<code>' header for all allowed methods
 * (see {@link org.searchisko.api.annotations.header.CORSSupport#allowedMethods()}).
 *
 * 'Access-Control-Max-Age' is set to 86400, this means pre-flight response can be cached for 24 hours.
 * 'Access-Control-Allow-Headers' is set to 'X-Requested-With, Content-Type, Content-Length'.
 * 'Access-Control-Allow-Credentials' is set to 'true'.
 *
 * @author Lukas Vlcek
 */
@Provider
@ServerInterceptor
@HeaderDecoratorPrecedence
public class CORSSupportInterceptor implements PostProcessInterceptor, AcceptedByMethod {

	@Inject
	protected Logger log;

	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

	private static class Header {
		protected String key;
		protected Object value;

		Header(String key, Object value) {
			this.key = key;
			this.value = value;
		}
	}

	private static final Map<Method, List<Header>> headers = new HashMap<Method, List<Header>>();

	private static List<Header> getHeaderList(Method method) {
		return headers.get(method);
	}

	// method must be synchronized as it modifies static level object
	private static synchronized void addIntoHeaderList(Method method, Header header) {
		if (!headers.containsKey(method)) {
			headers.put(method, new ArrayList<Header>());
		}
		getHeaderList(method).add(header);
	}

	protected static void addHeaders(Method method) {
		addIntoHeaderList(method, new Header(ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
		if (method.isAnnotationPresent(javax.ws.rs.OPTIONS.class)) {
			CORSSupport annotation = method.getAnnotation(CORSSupport.class);
			if (annotation != null) {
				// allow to cache pre-flight response for 24 hours
				addIntoHeaderList(method, new Header(ACCESS_CONTROL_MAX_AGE, "86400"));
				addIntoHeaderList(method, new Header(ACCESS_CONTROL_ALLOW_HEADERS, "X-Requested-With, Content-Type, Content-Length, Origin, Accept"));
				// TODO: should be used only if http://www.html5rocks.com/en/tutorials/cors/#toc-withcredentials
				addIntoHeaderList(method, new Header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
				if (annotation.allowedMethods() != null) {
					for (String m : annotation.allowedMethods()) {
						addIntoHeaderList(method, new Header(ACCESS_CONTROL_ALLOW_METHODS, m));
					}
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean accept(Class aClass, Method method) {

		boolean isAnnotated = method.isAnnotationPresent(CORSSupport.class);
		if (isAnnotated) {
			addHeaders(method);
		}
		return isAnnotated;
	}

	@Override
	public void postProcess(ServerResponse serverResponse) {
		List<Header> h = headers.get(serverResponse.getResourceMethod());
		if (h != null) {
			for (Header header : h) {
				serverResponse.getMetadata().add(header.key, header.value);
			}
		}
	}
}
