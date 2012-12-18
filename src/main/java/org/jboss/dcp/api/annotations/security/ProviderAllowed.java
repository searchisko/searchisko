/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.annotations.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for securing whole class or method to be accessible only to provider<br/>
 * Annotation can be used on class which secures all methods. If is used at method then it overrides class annotation.
 * 
 * @author Libor Krzyzanek
 * 
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ProviderAllowed {

	/**
	 * If only super provider is allowed. Default value is false.
	 * 
	 * @return
	 */
	boolean superProviderOnly() default false;

}
