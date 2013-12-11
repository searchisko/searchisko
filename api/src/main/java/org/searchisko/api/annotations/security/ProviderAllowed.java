/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.annotations.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.searchisko.api.rest.security.ProviderSecurityPreProcessInterceptor;

/**
 * Annotation for securing whole class or method to be accessible only for authenticated Provider.<br/>
 * Annotation can be used on class which secures all methods. If used at method level then overrides class annotation.
 * 
 * @author Libor Krzyzanek
 * @see ProviderSecurityPreProcessInterceptor
 * 
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProviderAllowed {

	/**
	 * Set to <code>true</code> if only super provider is allowed. Default value is <code>false</code>.
	 */
	boolean superProviderOnly() default false;

}
