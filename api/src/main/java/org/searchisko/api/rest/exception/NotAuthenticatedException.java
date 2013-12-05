/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.exception;

/**
 * Exception used when authenticated user is expected in REST handler but none is found. This generally means some
 * misconfiguration or code error because interceptors are used to control access to REST handlers.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class NotAuthenticatedException extends RuntimeException {

	/**
	 * Types of possible authenticated users - each part of API may require another type of user to be authenticated.
	 * 
	 * @author Vlastimil Elias (velias at redhat dot com)
	 */
	public static enum AuthenticatedUserTypes {
		PROVIDER, CONTRIBUTOR;
	}

	/**
	 * Constructor.
	 * 
	 * @param requiredType mandatory type of authenticated user we require.
	 */
	public NotAuthenticatedException(AuthenticatedUserTypes requiredType) {
		super(requiredType.name());
	}

}
