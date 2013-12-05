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
 * Annotation used for allowing any user to access method where annotation is used.
 * 
 * @author Libor Krzyzanek
 * @see ProviderSecurityPreProcessInterceptor
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GuestAllowed {

}
