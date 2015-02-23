/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.rest.exception;

/**
 * Exception used when user is not authorized to perform some action. Useful for fine grained authorization code.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class NotAuthorizedException extends RuntimeException {

	/**
	 * Constructor.
	 * 
	 * @param description of required authorization
	 */
	public NotAuthorizedException(String description) {
		super(description);
	}

}
