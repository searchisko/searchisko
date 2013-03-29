/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import org.jboss.dcp.api.annotations.header.CORSSupport;
import org.jboss.resteasy.annotations.interception.HeaderDecoratorPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PostProcessInterceptor;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * RestEasy interceptor which adds '<code>Access-Control-Allow-Origin: *<code>' header to JAX-RS responses
 * for JAX-RS methods annotated by {@link org.jboss.dcp.api.annotations.header.CORSSupport}.
 *
 * It can be used on any HTTP method (GET, PUT, POST, ... ) even on custom ones. If, however; used on OPTIONS
 * method then it adds also '<code>Access-Control-Allow-Methods<code>' header for all allowed methods
 * (see {@link org.jboss.dcp.api.annotations.header.CORSSupport#allowedMethods()}).
 * 
 * @author Lukas Vlcek
 */
@Provider
@ServerInterceptor
@HeaderDecoratorPrecedence
public class AccessControlPostProcessInterceptor implements PostProcessInterceptor, AcceptedByMethod {

    @Inject
    protected Logger log;

	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    private static class Header {
        protected String key;
        protected Object value;
        Header(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    private static volatile Map<Method, List<Header>> headers = new HashMap();

    private static List<Header> getHeaderList(Method method) {
        return headers.get(method);
    }

    // method must be synchronized as it modifies static level object
    private static synchronized void addIntoHeaderList(Method method, Header header) {
        if (!headers.containsKey(method)) {
            headers.put(method, new ArrayList());
        }
        getHeaderList(method).add(header);
    }

    protected static void addHeaders(Method method) {
        addIntoHeaderList(method, new Header(ACCESS_CONTROL_ALLOW_ORIGIN, "*"));
        if (method.isAnnotationPresent(javax.ws.rs.OPTIONS.class)) {
            CORSSupport annotation = method.getAnnotation(CORSSupport.class);
            if (annotation != null && annotation.allowedMethods() != null) {
                for (String m : annotation.allowedMethods()) {
                    addIntoHeaderList(method, new Header(ACCESS_CONTROL_ALLOW_METHODS, m));
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
