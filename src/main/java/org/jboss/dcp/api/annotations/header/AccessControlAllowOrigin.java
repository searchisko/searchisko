/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.annotations.header;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jboss.dcp.api.rest.AccessControlPostProcessInterceptor;

/**
 * Annotation used for add '<code>Access-Control-Allow-Origin: *'<code>' header of a JAX-RS response
 * to allow a simple cross-origin requests.
 * 
 * See <a href="http://www.w3.org/TR/cors/#simple-cross-origin-request">http://www.w3.org/TR/cors/#simple-cross-origin-request</a>
 * 
 * @author Lukas Vlcek
 * @see AccessControlPostProcessInterceptor
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControlAllowOrigin {

}
