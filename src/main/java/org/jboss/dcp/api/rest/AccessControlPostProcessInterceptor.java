/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.rest;

import java.lang.reflect.Method;

import javax.ws.rs.ext.Provider;

import org.jboss.dcp.api.annotations.header.AccessControlAllowOrigin;
import org.jboss.resteasy.annotations.interception.HeaderDecoratorPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PostProcessInterceptor;

/**
 * RestEasy interceptor which adds '
 * <code>Access-Control-Allow-Origin: *<code>' header to JAX-RS responses for JAX-RS methods annotated by {@link AccessControlAllowOrigin}.
 * 
 * @author Lukas Vlcek
 */
@Provider
@ServerInterceptor
@HeaderDecoratorPrecedence
public class AccessControlPostProcessInterceptor implements PostProcessInterceptor, AcceptedByMethod {

	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

	@SuppressWarnings("rawtypes")
	@Override
	public boolean accept(Class aClass, Method method) {
		return method.isAnnotationPresent(AccessControlAllowOrigin.class);
	}

	@Override
	public void postProcess(ServerResponse serverResponse) {
		serverResponse.getMetadata().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
	}
}
