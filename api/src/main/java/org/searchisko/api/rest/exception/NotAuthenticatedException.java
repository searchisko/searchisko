/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.exception;

import org.searchisko.api.security.AuthenticatedUserType;

/**
 * Exception used when authenticated user is expected in REST handler but none is found. This generally means some
 * misconfiguration or code error because interceptors are used to control access to REST handlers.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class NotAuthenticatedException extends RuntimeException {

	/**
	 * Constructor.
	 * 
	 * @param requiredType mandatory type of authenticated user we require.
	 */
	public NotAuthenticatedException(AuthenticatedUserType requiredType) {
		super(requiredType.name());
	}

}
