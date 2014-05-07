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


/**
 * Annotation for securing whole class or method to be accessible only for authenticated Contributor.<br/>
 * Annotation can be used on class which secures all methods. If used at method level then overrides class annotation.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 *
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContributorAllowed {

	//TODO: Replace ContributorAllowed by Role.CONTRIBUTOR role
	/**
	 * If <code>true</code> then access check is optional, it means even unauthenticated user is allowed to access method.
	 * But is allows to call getAuthenticatedContributor in this method to determine if authenticated or not (
	 * {@link ContributorAuthenticationInterceptor} is invoked). Default <code>false</code> for sure.
	 */
	boolean optional() default false;

}
